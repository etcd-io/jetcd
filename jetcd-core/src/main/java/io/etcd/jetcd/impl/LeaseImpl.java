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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedLeaseClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

/**
 * Implementation of lease client.
 */
final class LeaseImpl extends Impl implements Lease {
    private static final int FIRST_KEEPALIVE_TIMEOUT_MS = 5000;

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
            LeaseGrantResponse::new);
    }

    @Override
    public CompletableFuture<LeaseGrantResponse> grant(long ttl, long timeout, TimeUnit unit) {
        return execute(
            () -> this.stub.withDeadlineAfter(timeout, unit).leaseGrant(
                LeaseGrantRequest.newBuilder()
                    .setTTL(ttl)
                    .build()),
            LeaseGrantResponse::new);
    }

    @Override
    public CompletableFuture<LeaseRevokeResponse> revoke(long leaseId) {
        return execute(
            () -> this.stub.leaseRevoke(
                LeaseRevokeRequest.newBuilder()
                    .setID(leaseId)
                    .build()),
            LeaseRevokeResponse::new);
    }

    @Override
    public CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId, LeaseOption option) {
        checkNotNull(option, "LeaseOption should not be null");

        LeaseTimeToLiveRequest leaseTimeToLiveRequest = LeaseTimeToLiveRequest.newBuilder()
            .setID(leaseId)
            .setKeys(option.isAttachedKeys())
            .build();

        return execute(
            () -> this.stub.leaseTimeToLive(leaseTimeToLiveRequest),
            LeaseTimeToLiveResponse::new);
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
        final CompletableFuture<LeaseKeepAliveResponse> future = new CompletableFuture<>();

        final CloseableClient ka = keepAlive(leaseId, new StreamObserver<>() {
            @Override
            public void onNext(LeaseKeepAliveResponse value) {
                future.complete(value);
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(toEtcdException(t));
            }

            @Override
            public void onCompleted() {
            }
        });

        return future.whenCompleteAsync(
            (val, throwable) -> ka.close(),
            connectionManager().getExecutorService());
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
        private volatile ScheduledFuture<?> task;
        private volatile ScheduledFuture<?> restart;
        private volatile ScheduledExecutorService executor;
        private volatile WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest> requestStream;

        public KeepAlive() {
            this.executor = Executors.newScheduledThreadPool(2);
        }

        @Override
        public void doStart() {
            leaseStub.leaseKeepAlive(this::writeHandler)
                .handler(this::handleResponse)
                .exceptionHandler(this::handleException);
        }

        @Override
        public void doStop() {
            if (requestStream != null) {
                requestStream.end();
            }
            if (this.restart != null) {
                this.restart.cancel(true);
                this.restart = null;
            }
            if (this.task != null) {
                this.task.cancel(true);
                this.task = null;
            }
        }

        @Override
        public void close() {
            super.close();

            this.task = null;
            this.restart = null;
            this.executor.shutdownNow();
        }

        private void writeHandler(WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest> stream) {
            requestStream = stream;

            task = executor.scheduleAtFixedRate(
                () -> keepAlives.values().forEach(element -> sendKeepAlive(element, stream)),
                0,
                500,
                TimeUnit.MILLISECONDS);
        }

        private void sendKeepAlive(KeepAliveObserver observer, WriteStream<io.etcd.jetcd.api.LeaseKeepAliveRequest> stream) {
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

        private synchronized void handleException(Throwable r) {
            if (!this.isRunning()) {
                return;
            }

            restart = this.executor.schedule(this::restart, 500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * The DeadLiner hold a background task to check deadlines.
     */
    private class DeadLine extends Service {
        private volatile ScheduledFuture<?> task;
        private volatile ScheduledExecutorService executor;

        public DeadLine() {
            this.executor = Executors.newScheduledThreadPool(2);
        }

        @Override
        public void doStart() {
            this.task = executor.scheduleAtFixedRate(() -> {
                long now = System.currentTimeMillis();

                keepAlives.values().removeIf(ka -> {
                    if (ka.getDeadLine() < now) {
                        ka.onCompleted();
                        return true;
                    }
                    return false;
                });
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void doStop() {
            if (this.task != null) {
                this.task.cancel(true);
            }
        }

        @Override
        public void close() {
            super.close();

            this.executor.shutdownNow();
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
            this.nextKeepAlive = System.currentTimeMillis();
            this.deadLine = nextKeepAlive + FIRST_KEEPALIVE_TIMEOUT_MS;

            this.observers = new CopyOnWriteArrayList<>();
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
