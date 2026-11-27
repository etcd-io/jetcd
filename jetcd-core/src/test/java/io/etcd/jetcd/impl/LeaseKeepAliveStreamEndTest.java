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

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.test.EtcdConstants;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.ThreadingModel;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@Timeout(15)
class LeaseKeepAliveStreamEndTest {
    private final AtomicInteger streamsOpened = new AtomicInteger();
    private Vertx vertx;
    private Client client;

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            if (vertx != null) {
                vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void renewsExistingAndNewLeasesAfterCleanEnd() throws Exception {
        Lease lease = startServer(true);
        var renewals = new AtomicInteger();
        var listenerError = new AtomicReference<Throwable>();
        var secondLeaseRenewed = new CompletableFuture<LeaseKeepAliveResponse>();

        lease.keepAlive(1, response -> renewals.incrementAndGet(), listenerError::set);
        await().until(() -> renewals.get() > 0);
        lease.keepAlive(2, secondLeaseRenewed::complete, listenerError::set);

        assertThat(secondLeaseRenewed.get(5, TimeUnit.SECONDS).getID()).isEqualTo(2);
        await().during(Duration.ofMillis(750)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(renewals).hasValueGreaterThan(1);
            assertThat(streamsOpened).hasValue(2);
            assertThat(listenerError).hasValue(null);
        });
    }

    @Test
    void closeDoesNotReopenTheStream() throws Exception {
        Lease lease = startServer(true);
        var renewed = new CompletableFuture<LeaseKeepAliveResponse>();

        lease.keepAlive(1, renewed::complete, renewed::completeExceptionally);
        renewed.get(5, TimeUnit.SECONDS);
        lease.close();

        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
            .until(() -> streamsOpened.get() == 1);
    }

    @Test
    void keepAliveOnceFailsWhenTheStreamEndsWithoutAReply() throws Exception {
        Lease lease = startServer(false);

        assertThatThrownBy(() -> lease.keepAliveOnce(1).get(5, TimeUnit.SECONDS))
            .cause()
            .isInstanceOfSatisfying(EtcdException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UNAVAILABLE));
    }

    @Test
    void blockedListenerKeepsOnlyLatestHeartbeatWhileOtherListenersContinue() throws Exception {
        var renewals = new HashMap<Long, Integer>();
        Lease lease = startServer(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(2), request -> {
            request.handler(message -> {
                long leaseId = message.getID();
                int renewal = renewals.merge(leaseId, 1, Integer::sum);
                int lastRenewal = leaseId == 1 ? 3 : 2;
                request.response().write(heartbeat(leaseId, renewal, renewal == lastRenewal ? 30 : 3));
            });
            request.endHandler(ignored -> request.response().end());
        });

        var slowStarted = new CompletableFuture<Boolean>();
        var releaseSlow = new CompletableFuture<Void>();
        var slowCaughtUp = new CompletableFuture<Void>();
        var fastSameLease = new CompletableFuture<Void>();
        var fastOtherLease = new CompletableFuture<Void>();
        var slowRevisions = new ConcurrentLinkedQueue<Long>();
        lease.keepAlive(1, response -> {
            long revision = response.getHeader().getRevision();
            slowRevisions.add(revision);
            if (revision == 1) {
                slowStarted.complete(Context.isOnWorkerThread());
                releaseSlow.orTimeout(10, TimeUnit.SECONDS).join();
            } else if (revision == 3) {
                slowCaughtUp.complete(null);
            }
        });

        try {
            assertThat(slowStarted.get(5, TimeUnit.SECONDS)).isTrue();
            lease.keepAlive(1, response -> {
                if (response.getHeader().getRevision() == 3) {
                    fastSameLease.complete(null);
                }
            }, fastSameLease::completeExceptionally);
            lease.keepAlive(2, response -> {
                if (response.getHeader().getRevision() == 2) {
                    fastOtherLease.complete(null);
                }
            }, fastOtherLease::completeExceptionally);
            CompletableFuture.allOf(fastSameLease, fastOtherLease).get(6, TimeUnit.SECONDS);
            assertThat(slowRevisions).containsExactly(1L);
        } finally {
            releaseSlow.complete(null);
        }
        slowCaughtUp.get(5, TimeUnit.SECONDS);
        assertThat(slowRevisions).containsExactly(1L, 3L);
    }

    @Test
    void deliversPendingHeartbeatBeforeLeaseNotFound() throws Exception {
        Lease lease = startServer(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(2), request -> {
            request.handler(message -> {
                if (message.getID() == 1) {
                    request.response().write(heartbeat(1, 1, 30))
                        .compose(ignored -> request.response().write(heartbeat(1, 2, 30)))
                        .compose(ignored -> request.response().write(heartbeat(1, 3, 0)));
                } else {
                    request.response().write(heartbeat(2, 1, 30));
                }
            });
            request.endHandler(ignored -> request.response().end());
        });

        var slowStarted = new CompletableFuture<Void>();
        var releaseSlow = new CompletableFuture<Void>();
        var otherLeaseRenewed = new CompletableFuture<Void>();
        var terminal = new CompletableFuture<Throwable>();
        var revisions = new ConcurrentLinkedQueue<Long>();
        lease.keepAlive(1, response -> {
            long revision = response.getHeader().getRevision();
            revisions.add(revision);
            if (revision == 1) {
                slowStarted.complete(null);
                releaseSlow.orTimeout(10, TimeUnit.SECONDS).join();
            }
        }, terminal::complete, () -> terminal.complete(null));

        try {
            slowStarted.get(5, TimeUnit.SECONDS);
            lease.keepAlive(2, response -> otherLeaseRenewed.complete(null), otherLeaseRenewed::completeExceptionally);
            otherLeaseRenewed.get(5, TimeUnit.SECONDS);
            assertThat(terminal).isNotDone();
        } finally {
            releaseSlow.complete(null);
        }
        assertThat(terminal.get(5, TimeUnit.SECONDS)).isInstanceOfSatisfying(EtcdException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThat(revisions).containsExactly(1L, 2L);
    }

    @Test
    void blockedListenerReceivesOnlyLatestPendingReconnectErrorBeforeTermination() throws Exception {
        vertx = Vertx.vertx();
        var started = new CompletableFuture<Void>();
        var release = new CompletableFuture<Void>();
        var callbacks = new ConcurrentLinkedQueue<String>();
        var initialError = new IllegalStateException("initial reconnect error");
        var terminalError = new IllegalStateException("lease not found");
        var listener = new LeaseKeepAliveListener(vertx, Lease.listener(
            response -> callbacks.add("heartbeat " + response.getHeader().getRevision()),
            error -> {
                callbacks.add(error.getMessage());
                if (initialError.equals(error)) {
                    started.complete(null);
                    release.orTimeout(10, TimeUnit.SECONDS).join();
                }
            },
            () -> callbacks.add("completed")));

        listener.onReconnectError(initialError);
        try {
            started.get(5, TimeUnit.SECONDS);
            listener.onReconnectError(new IllegalStateException("superseded reconnect error"));
            listener.onNext(new LeaseKeepAliveResponse(heartbeat(1, 2, 30)));
            listener.onReconnectError(new IllegalStateException("latest reconnect error"));
            listener.terminate(registered -> registered.onError(terminalError));
            assertThat(callbacks).containsExactly("initial reconnect error");
        } finally {
            release.complete(null);
        }

        listener.drained().get(5, TimeUnit.SECONDS);
        assertThat(callbacks).containsExactly("initial reconnect error", "heartbeat 2", "latest reconnect error",
            "lease not found");
    }

    @Test
    void renewsWhenStartedFromTheOnlyWorkerAndAllowsReentrantClose() throws Exception {
        var callbackStarted = new CompletableFuture<Void>();
        var releaseCallback = new CompletableFuture<Void>();
        var renewedWhileBlocked = new CompletableFuture<Void>();
        Lease lease = startServer(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(1), request -> {
            request.handler(message -> {
                if (callbackStarted.isDone() && !releaseCallback.isDone()) {
                    renewedWhileBlocked.complete(null);
                }
                request.response().write(heartbeat(message.getID(), 1, 3));
            });
            request.endHandler(ignored -> request.response().end());
        });

        var returnedFromClose = new CompletableFuture<Void>();
        var terminal = new CompletableFuture<Throwable>();
        try {
            vertx.deployVerticle(new VerticleBase() {
                @Override
                public Future<?> start() {
                    lease.keepAlive(1, response -> {
                        if (callbackStarted.complete(null)) {
                            releaseCallback.orTimeout(10, TimeUnit.SECONDS).join();
                            lease.close();
                            returnedFromClose.complete(null);
                        }
                    }, terminal::complete);
                    return Future.succeededFuture();
                }
            }, new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
                .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            callbackStarted.get(5, TimeUnit.SECONDS);
            renewedWhileBlocked.get(5, TimeUnit.SECONDS);
            assertThat(terminal).isNotDone();
        } finally {
            releaseCallback.complete(null);
        }
        returnedFromClose.get(5, TimeUnit.SECONDS);
        assertThat(terminal.get(5, TimeUnit.SECONDS)).isInstanceOf(ClosedClientException.class);
    }

    private Lease startServer(boolean respondOnFirstStream) throws Exception {
        return startServer(new VertxOptions(), request -> {
            boolean first = streamsOpened.incrementAndGet() == 1;
            request.handler(message -> {
                var response = heartbeat(message.getID(), 1, 3);
                if (!first) {
                    request.response().write(response);
                } else if (respondOnFirstStream) {
                    request.response().end(response);
                } else {
                    request.response().end();
                }
            });
            if (!first) {
                request.endHandler(ignored -> request.response().end());
            }
        });
    }

    private Lease startServer(VertxOptions options,
        Handler<GrpcServerRequest<io.etcd.jetcd.api.LeaseKeepAliveRequest, io.etcd.jetcd.api.LeaseKeepAliveResponse>> handler)
        throws Exception {
        vertx = Vertx.vertx(options);
        var keepAlive = io.etcd.jetcd.api.LeaseGrpcClient.LeaseKeepAlive;
        ServiceMethod<io.etcd.jetcd.api.LeaseKeepAliveRequest, io.etcd.jetcd.api.LeaseKeepAliveResponse> method = ServiceMethod
            .server(keepAlive.serviceName(), keepAlive.methodName(), keepAlive.cardinality(), GrpcMessageEncoder.encoder(),
                GrpcMessageDecoder.decoder(io.etcd.jetcd.api.LeaseKeepAliveRequest.getDefaultInstance()));
        var grpcServer = GrpcServer.server(vertx).callHandler(method, handler);
        var server = vertx.createHttpServer().requestHandler(grpcServer).listen(0, EtcdConstants.LOCALHOST)
            .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

        client = Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + server.actualPort()).vertx(vertx).build();
        return client.getLeaseClient();
    }

    private static io.etcd.jetcd.api.LeaseKeepAliveResponse heartbeat(long leaseId, long revision, long ttl) {
        return io.etcd.jetcd.api.LeaseKeepAliveResponse.newBuilder()
            .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setRevision(revision))
            .setID(leaseId)
            .setTTL(ttl)
            .build();
    }
}
