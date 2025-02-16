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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.etcd.jetcd.Lease;
import io.etcd.jetcd.api.LeaseGrantRequest;
import io.etcd.jetcd.api.LeaseKeepAliveRequest;
import io.etcd.jetcd.api.LeaseRevokeRequest;
import io.etcd.jetcd.api.LeaseTimeToLiveRequest;
import io.etcd.jetcd.api.VertxLeaseGrpc;
import io.etcd.jetcd.common.Service;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.support.Util;
import io.grpc.stub.StreamObserver;
import io.vertx.core.streams.WriteStream;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedLeaseClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of lease client.
 */
final class LeaseImpl extends Impl implements Lease {

    /**
     * if there is no user-provided keep-alive timeout from ClientBuilder, then DEFAULT_FIRST_KEEPALIVE_TIMEOUT_MS
     * is the timeout for the first keepalive request before the actual TTL is known to the lease client.
     */
    private static final int DEFAULT_FIRST_KEEPALIVE_TIMEOUT_MS = 5000;

    private final VertxLeaseGrpc.LeaseVertxStub stub;
    private final VertxLeaseGrpc.LeaseVertxStub leaseStub;
    private final Map<Long, KeepAliveObserver> keepAlives;
    private final KeepAlive keepAlive;
    private final DeadLine deadLine;
    private volatile boolean closed;

    LeaseImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager().newStub(VertxLeaseGrpc::newVertxStub);
        this.leaseStub = Util.applyRequireLeader(true, connectionManager().newStub(VertxLeaseGrpc::newVertxStub));
        this.keepAlives = new ConcurrentHashMap<>();
        this.keepAlive = new KeepAlive();
        this.deadLine = new DeadLine();
    }

    @Override
    public CompletableFuture<LeaseGrantResponse> grant(long ttl) {
        return execute(
            () -> this.stub.leaseGrant(
                LeaseGrantRequest.newBuilder()
                    .setTTL(ttl)
                    .build()),
            LeaseGrantResponse::new,
            true);
    }

    @Override
    public CompletableFuture<LeaseGrantResponse> grant(long ttl, long timeout, TimeUnit unit) {
        return execute(
            () -> this.stub.withDeadlineAfter(timeout, unit).leaseGrant(
                LeaseGrantRequest.newBuilder()
                    .setTTL(ttl)
                    .build()),
            LeaseGrantResponse::new,
            true);
    }

    @Override
    public CompletableFuture<LeaseRevokeResponse> revoke(long leaseId) {
        return execute(
            () -> this.stub.leaseRevoke(
                LeaseRevokeRequest.newBuilder()
                    .setID(leaseId)
                    .build()),
            LeaseRevokeResponse::new,
            true);
    }

    @Override
    public CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId, LeaseOption option) {
        requireNonNull(option, "LeaseOption should not be null");

        LeaseTimeToLiveRequest leaseTimeToLiveRequest = LeaseTimeToLiveRequest.newBuilder()
            .setID(leaseId)
            .setKeys(option.isAttachedKeys())
            .build();

        return execute(
            () -> this.stub.leaseTimeToLive(leaseTimeToLiveRequest),
            LeaseTimeToLiveResponse::new,
            true);
    }

    @Override
    public synchronized CloseableClient keepAlive(long leaseId, StreamObserver<LeaseKeepAliveResponse> observer) {
        if (this.closed) {
            throw newClosedLeaseClientException();
        }

        KeepAliveObserver keepAlive = this.keepAlives.computeIfAbsent(leaseId, KeepAliveObserver::new);
        keepAlive.addObserver(observer);

        this.keepAlive.start();
        this.deadLine.start();

        return new CloseableClient() {
            @Override
            public void close() {
                keepAlive.removeObserver(observer);
            }
        };
    }

    @Override
    public CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId) {
        final AtomicReference<WriteStream<LeaseKeepAliveRequest>> ref = new AtomicReference<>();
        final CompletableFuture<LeaseKeepAliveResponse> future = new CompletableFuture<>();
        final LeaseKeepAliveRequest req = LeaseKeepAliveRequest.newBuilder().setID(leaseId).build();

        leaseStub
            .leaseKeepAliveWithHandler(
                s -> {
                    ref.set(s);
                    s.write(req);
                },
                r -> {
                    if (r.getTTL() != 0) {
                        future.complete(new LeaseKeepAliveResponse(r));
                    } else {
                        future.completeExceptionally(
                            newEtcdException(ErrorCode.NOT_FOUND, "etcdserver: requested lease not found"));
                    }
                },
                null,
                future::completeExceptionally);

        return future.whenComplete((r, t) -> ref.get().end(req));
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;

        this.keepAlive.close();
        this.deadLine.close();

        final Throwable errResp = newClosedLeaseClientException();

        this.keepAlives.values().forEach(v -> v.onError(errResp));
        this.keepAlives.clear();
    }

    /**
     * The KeepAliver hold a background task and stream for keep aliaves.
     */
    private final class KeepAlive extends Service {
        private volatile Long task;
        private volatile Long restart;
        private volatile WriteStream<LeaseKeepAliveRequest> requestStream;

        public KeepAlive() {
        }

        @Override
        public void doStart() {
            leaseStub.leaseKeepAliveWithHandler(
                this::writeHandler,
                this::handleResponse,
                null,
                this::handleException);
        }

        @Override
        public void doStop() {
            if (requestStream != null) {
                requestStream.end();
            }
            if (this.restart != null) {
                connectionManager().vertx().cancelTimer(this.restart);
            }
            if (this.task != null) {
                connectionManager().vertx().cancelTimer(this.task);
            }
        }

        @Override
        public void close() {
            super.close();

            this.task = null;
            this.restart = null;
        }

        private void writeHandler(WriteStream<LeaseKeepAliveRequest> stream) {
            requestStream = stream;

            task = connectionManager().vertx().setPeriodic(
                0,
                500,
                l -> {
                    keepAlives.values().forEach(element -> sendKeepAlive(element, stream));
                });
        }

        private void sendKeepAlive(KeepAliveObserver observer, WriteStream<LeaseKeepAliveRequest> stream) {
            if (observer.getNextKeepAlive() < System.currentTimeMillis()) {
                stream.write(
                    LeaseKeepAliveRequest.newBuilder().setID(observer.getLeaseId()).build());
            }
        }

        private synchronized void handleResponse(io.etcd.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse) {
            if (!this.isRunning()) {
                return;
            }

            final long leaseID = leaseKeepAliveResponse.getID();
            final long ttl = leaseKeepAliveResponse.getTTL();
            final KeepAliveObserver ka = keepAlives.get(leaseID);

            if (ka == null) {
                return;
            }

            if (ttl > 0) {
                long nextKeepAlive = System.currentTimeMillis() + ttl * 1000 / 3;
                ka.setNextKeepAlive(nextKeepAlive);
                ka.setDeadLine(System.currentTimeMillis() + ttl * 1000);
                ka.onNext(leaseKeepAliveResponse);
            } else {
                keepAlives.remove(leaseID);
                ka.onError(newEtcdException(ErrorCode.NOT_FOUND, "etcdserver: requested lease not found"));
            }
        }

        private synchronized void handleException(Throwable throwable) {
            if (!this.isRunning()) {
                return;
            }

            keepAlives.values().forEach(ka -> ka.onError(throwable));

            restart = connectionManager().vertx().setTimer(
                500,
                l -> {
                    if (isRunning()) {
                        restart();
                    }
                });
        }
    }

    /**
     * The DeadLiner hold a background task to check deadlines.
     */
    private class DeadLine extends Service {
        private volatile Long task;

        public DeadLine() {
        }

        @Override
        public void doStart() {
            this.task = connectionManager().vertx().setPeriodic(
                0,
                1000,
                l -> {
                    long now = System.currentTimeMillis();

                    keepAlives.values().removeIf(ka -> {
                        if (ka.getDeadLine() < now) {
                            ka.onCompleted();
                            return true;
                        }
                        return false;
                    });
                });
        }

        @Override
        public void doStop() {
            if (this.task != null) {
                connectionManager().vertx().cancelTimer(this.task);
            }
        }
    }

    /**
     * The KeepAlive hold the keepAlive information for lease.
     */
    private final class KeepAliveObserver implements StreamObserver<io.etcd.jetcd.api.LeaseKeepAliveResponse> {
        private final List<StreamObserver<LeaseKeepAliveResponse>> observers;
        private final long leaseId;

        private long deadLine;
        private long nextKeepAlive;

        public KeepAliveObserver(long leaseId) {
            this(leaseId, Collections.emptyList());
        }

        public KeepAliveObserver(long leaseId, Collection<StreamObserver<LeaseKeepAliveResponse>> observers) {
            this.nextKeepAlive = System.currentTimeMillis();

            // Use user-provided timeout if present to avoid removing KeepAlive before first response from server
            int initialKeepAliveTimeoutMs = connectionManager().builder().keepaliveTimeout() != null
                ? Math.toIntExact(connectionManager().builder().keepaliveTimeout().toMillis())
                : DEFAULT_FIRST_KEEPALIVE_TIMEOUT_MS;
            this.deadLine = nextKeepAlive + initialKeepAliveTimeoutMs;

            this.observers = new CopyOnWriteArrayList<>(observers);
            this.leaseId = leaseId;
        }

        public long getLeaseId() {
            return leaseId;
        }

        public long getDeadLine() {
            return deadLine;
        }

        public void setDeadLine(long deadLine) {
            this.deadLine = deadLine;
        }

        public void addObserver(StreamObserver<LeaseKeepAliveResponse> observer) {
            this.observers.add(observer);
        }

        public void removeObserver(StreamObserver<LeaseKeepAliveResponse> listener) {
            this.observers.remove(listener);

            if (this.observers.isEmpty()) {
                keepAlives.remove(leaseId);
            }
        }

        public long getNextKeepAlive() {
            return nextKeepAlive;
        }

        public void setNextKeepAlive(long nextKeepAlive) {
            this.nextKeepAlive = nextKeepAlive;
        }

        @Override
        public void onNext(io.etcd.jetcd.api.LeaseKeepAliveResponse response) {
            for (StreamObserver<LeaseKeepAliveResponse> observer : observers) {
                observer.onNext(new LeaseKeepAliveResponse(response));
            }
        }

        @Override
        public void onError(Throwable throwable) {
            for (StreamObserver<LeaseKeepAliveResponse> observer : observers) {
                observer.onError(toEtcdException(throwable));
            }
        }

        @Override
        public void onCompleted() {
            this.observers.forEach(StreamObserver::onCompleted);
            this.observers.clear();
        }
    }
}
