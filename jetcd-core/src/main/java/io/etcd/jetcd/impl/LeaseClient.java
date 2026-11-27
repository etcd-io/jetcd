/*
 * Copyright 2016-2021 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.Lease;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.support.CloseableClient;
import io.vertx.core.Context;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedLeaseClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of lease client.
 */
final class LeaseClient extends AbstractClient implements Lease {

    private static final Logger LOG = LoggerFactory.getLogger(LeaseClient.class);
    private static final int DEFAULT_FIRST_KEEPALIVE_TIMEOUT_MS = 5000;
    private static final long LISTENER_CLOSE_TIMEOUT_SECONDS = 10;

    private final io.etcd.jetcd.api.LeaseGrpcClient client;
    private final KeepAlive keepAlive;

    LeaseClient(GrpcService grpcService) {
        super(grpcService);

        this.client = io.etcd.jetcd.api.LeaseGrpcClient.create(
            grpcService.getAuthenticatedGrpcClient(),
            grpcService.getServiceResolver().getTarget());
        this.keepAlive = new KeepAlive();
    }

    @Override
    public CompletableFuture<LeaseGrantResponse> grant(long ttl) {
        return execute(
            io.etcd.jetcd.api.LeaseGrpcClient.LeaseGrant,
            grantRequest(ttl),
            responseFactory::newLeaseGrantResponse,
            false);
    }

    @Override
    public CompletableFuture<LeaseGrantResponse> grant(long ttl, long timeout, TimeUnit unit) {
        return executeWithDeadline(
            io.etcd.jetcd.api.LeaseGrpcClient.LeaseGrant,
            grantRequest(ttl),
            responseFactory::newLeaseGrantResponse,
            timeout,
            requireNonNull(unit, "unit should not be null"));
    }

    private static io.etcd.jetcd.api.LeaseGrantRequest grantRequest(long ttl) {
        return io.etcd.jetcd.api.LeaseGrantRequest.newBuilder().setTTL(ttl).build();
    }

    @Override
    public CompletableFuture<LeaseRevokeResponse> revoke(long leaseId) {
        return execute(
            io.etcd.jetcd.api.LeaseGrpcClient.LeaseRevoke,
            io.etcd.jetcd.api.LeaseRevokeRequest.newBuilder()
                .setID(leaseId)
                .build(),
            responseFactory::newLeaseRevokeResponse,
            true);
    }

    @Override
    public CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId, LeaseOption option) {
        requireNonNull(option, "LeaseOption should not be null");

        io.etcd.jetcd.api.LeaseTimeToLiveRequest leaseTimeToLiveRequest = io.etcd.jetcd.api.LeaseTimeToLiveRequest.newBuilder()
            .setID(leaseId)
            .setKeys(option.attachedKeys())
            .build();

        return execute(
            io.etcd.jetcd.api.LeaseGrpcClient.LeaseTimeToLive,
            leaseTimeToLiveRequest,
            responseFactory::newLeaseTimeToLiveResponse,
            true);
    }

    @Override
    public CloseableClient keepAlive(long leaseId, Lease.Listener listener) {
        LeaseKeepAliveListener registration = keepAlive.register(leaseId, listener);

        return new CloseableClient() {
            @Override
            public void close() {
                keepAlive.unregister(leaseId, registration);
            }
        };
    }

    @Override
    public CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId) {
        final AtomicReference<WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest>> writeStreamRef = new AtomicReference<>();
        final CompletableFuture<LeaseKeepAliveResponse> future = new CompletableFuture<>();
        final io.etcd.jetcd.api.LeaseKeepAliveRequest req = io.etcd.jetcd.api.LeaseKeepAliveRequest.newBuilder().setID(leaseId)
            .build();

        client.leaseKeepAlive((writeStream, err) -> {
            if (err != null) {
                future.completeExceptionally(err);
            } else {
                writeStreamRef.set(writeStream);
                writeStream.write(req);
            }
        }).onComplete(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                cleanupKeepAliveOnce(null, writeStreamRef.getAndSet(null));
            } else {
                ReadStream<io.etcd.jetcd.api.LeaseKeepAliveResponse> readStream = ar.result();
                readStream.pause();

                readStream.endHandler(ignored -> {
                    try {
                        future.completeExceptionally(
                            newEtcdException(ErrorCode.UNAVAILABLE, "keep alive stream ended without a response"));
                    } finally {
                        cleanupKeepAliveOnce(readStream, writeStreamRef.getAndSet(null));
                    }
                });
                readStream.exceptionHandler(t -> {
                    try {
                        future.completeExceptionally(t);
                    } finally {
                        cleanupKeepAliveOnce(readStream, writeStreamRef.getAndSet(null));
                    }
                });
                readStream.handler(r -> {
                    try {
                        if (r.getTTL() != 0) {
                            future.complete(new LeaseKeepAliveResponse(r));
                        } else {
                            future.completeExceptionally(
                                newEtcdException(ErrorCode.NOT_FOUND, "etcdserver: requested lease not found"));
                        }
                    } finally {
                        cleanupKeepAliveOnce(readStream, writeStreamRef.getAndSet(null));
                    }
                });
                readStream.resume();
            }
        });

        return future;
    }

    private void cleanupKeepAliveOnce(
        ReadStream<io.etcd.jetcd.api.LeaseKeepAliveResponse> readStream,
        WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest> writeStream) {

        if (readStream != null) {
            readStream.handler(null);
            readStream.exceptionHandler(null);
            readStream.endHandler(null);
        }

        if (writeStream != null) {
            writeStream.end();
        }
    }

    @Override
    public void close() {
        CompletableFuture<Void> notifications = keepAlive.close();
        // A listener may close the lease client from its own callback.
        if (Context.isOnVertxThread()) {
            return;
        }
        try {
            notifications.get(LISTENER_CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException error) {
            LOG.warn("Error waiting for lease keepalive listeners to close", error);
        }
    }

    private final class KeepAlive {
        private final Map<Long, KeepAliveObserver> observers = new HashMap<>();
        private final Set<LeaseKeepAliveListener> pendingTerminations = ConcurrentHashMap.newKeySet();
        private KeepAliveStream activeStream;
        private Long restartTimer;
        private Long deadlineTimer;
        private CompletableFuture<Void> closeFuture;
        private boolean closed;

        synchronized LeaseKeepAliveListener register(long leaseId, Lease.Listener listener) {
            if (closed) {
                throw newClosedLeaseClientException();
            }
            var registration = new LeaseKeepAliveListener(grpc().vertx(), requireNonNull(listener, "listener"));
            observers.computeIfAbsent(leaseId, KeepAliveObserver::new).listeners.add(registration);
            if (activeStream == null && restartTimer == null) {
                startOnEventLoop();
            }
            return registration;
        }

        synchronized void unregister(long leaseId, LeaseKeepAliveListener registration) {
            KeepAliveObserver observer = observers.get(leaseId);
            if (observer != null && observer.listeners.remove(registration) && observer.listeners.isEmpty()) {
                observers.remove(leaseId);
            }
            registration.cancel();
        }

        private void startOnEventLoop() {
            var vertx = grpc().vertx();
            Context context = vertx.getOrCreateContext();
            if (context.isEventLoopContext()) {
                context.runOnContext(ignored -> startForRegisteredListeners());
            } else {
                // Worker callers can share the same pool as blocked listeners.
                var unused = CompletableFuture.runAsync(() -> vertx.runOnContext(ignored -> startForRegisteredListeners()));
            }
        }

        private synchronized void startForRegisteredListeners() {
            if (!closed && !observers.isEmpty()) {
                start();
                if (deadlineTimer == null) {
                    deadlineTimer = grpc().vertx().setPeriodic(0, 1000, ignored -> expireLeases());
                }
            }
        }

        private void start() {
            if (closed || activeStream != null || restartTimer != null) {
                return;
            }

            KeepAliveStream stream = new KeepAliveStream();
            activeStream = stream;
            client.leaseKeepAlive((writeStream, error) -> {
                if (error == null) {
                    startSending(stream, writeStream);
                }
            })
                .onSuccess(readStream -> {
                    readStream.pause();
                    readStream.endHandler(ignored -> handleStreamEnd(stream, null));
                    readStream.exceptionHandler(error -> handleStreamEnd(stream, error));
                    readStream.handler(response -> handleResponse(stream, response));
                    readStream.resume();
                })
                .onFailure(error -> handleStreamEnd(stream, error));
        }

        synchronized CompletableFuture<Void> close() {
            if (closed) {
                return closeFuture;
            }
            closed = true;
            if (restartTimer != null) {
                grpc().vertx().cancelTimer(restartTimer);
                restartTimer = null;
            }
            if (activeStream != null) {
                closeStream(activeStream);
            }
            if (deadlineTimer != null) {
                grpc().vertx().cancelTimer(deadlineTimer);
                deadlineTimer = null;
            }

            var error = newClosedLeaseClientException();
            observers.values().forEach(observer -> terminate(observer, listener -> listener.onError(error)));
            observers.clear();
            closeFuture = CompletableFuture.allOf(pendingTerminations.stream()
                .map(LeaseKeepAliveListener::drained)
                .toArray(CompletableFuture[]::new));
            return closeFuture;
        }

        private synchronized void startSending(KeepAliveStream stream,
            WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest> requestStream) {
            if (closed || activeStream != stream) {
                requestStream.end();
                return;
            }

            stream.requestStream = requestStream;
            stream.sendTimer = grpc().vertx().setPeriodic(0, 500, ignored -> sendKeepAlives(stream));
        }

        private synchronized void sendKeepAlives(KeepAliveStream stream) {
            if (!closed && activeStream == stream) {
                observers.values().forEach(observer -> sendKeepAlive(observer, stream.requestStream));
            }
        }

        private void sendKeepAlive(KeepAliveObserver observer, WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest> stream) {
            if (observer.nextKeepAlive < System.currentTimeMillis()) {
                stream.write(
                    io.etcd.jetcd.api.LeaseKeepAliveRequest.newBuilder().setID(observer.leaseId).build());
            }
        }

        private synchronized void handleResponse(KeepAliveStream stream,
            io.etcd.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse) {
            if (closed || activeStream != stream) {
                return;
            }
            final long leaseID = leaseKeepAliveResponse.getID();
            final long ttl = leaseKeepAliveResponse.getTTL();
            KeepAliveObserver observer = observers.get(leaseID);
            if (observer == null) {
                return;
            }

            if (ttl > 0) {
                long now = System.currentTimeMillis();
                observer.nextKeepAlive = now + ttl * 1000 / 3;
                observer.deadline = now + ttl * 1000;
                var response = new LeaseKeepAliveResponse(leaseKeepAliveResponse);
                observer.listeners.forEach(listener -> listener.onNext(response));
            } else {
                observers.remove(leaseID);
                var error = newEtcdException(ErrorCode.NOT_FOUND, "etcdserver: requested lease not found");
                terminate(observer, listener -> listener.onError(error));
            }
        }

        private synchronized void handleStreamEnd(KeepAliveStream stream, Throwable error) {
            if (closed || activeStream != stream) {
                return;
            }
            closeStream(stream);
            restartTimer = grpc().vertx().setTimer(500, this::reopen);

            if (error != null) {
                var failure = toEtcdException(error);
                observers.values()
                    .forEach(observer -> observer.listeners.forEach(listener -> listener.onReconnectError(failure)));
            }
        }

        private synchronized void reopen(long timer) {
            if (restartTimer != null && restartTimer == timer) {
                restartTimer = null;
                if (!observers.isEmpty()) {
                    start();
                }
            }
        }

        private void closeStream(KeepAliveStream stream) {
            activeStream = null;
            if (stream.sendTimer != null) {
                grpc().vertx().cancelTimer(stream.sendTimer);
            }
            if (stream.requestStream != null) {
                stream.requestStream.end();
            }
        }

        private synchronized void expireLeases() {
            long now = System.currentTimeMillis();
            observers.values().removeIf(observer -> {
                if (observer.deadline >= now) {
                    return false;
                }
                terminate(observer, Lease.Listener::onCompleted);
                return true;
            });
        }

        private void terminate(KeepAliveObserver observer, Consumer<Lease.Listener> callback) {
            observer.listeners.forEach(listener -> {
                pendingTerminations.add(listener);
                listener.terminate(callback);
                var unused = listener.drained().whenComplete((ignored, error) -> pendingTerminations.remove(listener));
            });
        }
    }

    private static final class KeepAliveStream {
        private WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest> requestStream;
        private Long sendTimer;
    }

    private final class KeepAliveObserver {
        private final List<LeaseKeepAliveListener> listeners = new ArrayList<>();
        private final long leaseId;

        private long deadline;
        private long nextKeepAlive;

        KeepAliveObserver(long leaseId) {
            this.nextKeepAlive = System.currentTimeMillis();

            var timeout = grpc().builder().leaseKeepAliveTimeout();
            int initialKeepAliveTimeoutMs = timeout != null
                ? Math.toIntExact(timeout.toMillis())
                : DEFAULT_FIRST_KEEPALIVE_TIMEOUT_MS;
            this.deadline = nextKeepAlive + initialKeepAliveTimeoutMs;
            this.leaseId = leaseId;
        }
    }
}
