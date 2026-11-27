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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Election;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.test.EtcdConstants;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcErrorException;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerResponse;

import com.google.protobuf.ByteString;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(30)
class ElectionStreamingTest {
    private static final String OBSERVE_FAILURE = "election observation is unavailable";

    private final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(2));

    @AfterEach
    void closeVertx() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @EnumSource(names = { "OK", "UNAVAILABLE" })
    void deliversProclamationsInOrderWithoutBlockingKv(GrpcStatus status) throws Exception {
        var firstStarted = new CompletableFuture<Void>();
        var releaseFirst = new CompletableFuture<Void>();
        var secondSent = new CompletableFuture<Void>();
        var proposals = new ConcurrentLinkedQueue<String>();
        var end = new CompletableFuture<Throwable>();
        var proposalsAtEnd = new CompletableFuture<List<String>>();
        var terminalOnWorker = new CompletableFuture<Boolean>();
        var terminalCalls = new AtomicInteger();

        try (Client client = startServer(response -> {
            var firstCallbackStarted = Future.fromCompletionStage(firstStarted, Vertx.currentContext());
            response.write(proclamation("first")).compose(ignored -> firstCallbackStarted)
                .compose(ignored -> response.write(proclamation("second")))
                .compose(ignored -> {
                    if (status == GrpcStatus.UNAVAILABLE) {
                        response.statusMessage(OBSERVE_FAILURE);
                    }
                    return response.status(status).end();
                })
                .onComplete(sent -> {
                    if (sent.succeeded()) {
                        secondSent.complete(null);
                    } else {
                        secondSent.completeExceptionally(sent.cause());
                    }
                });
        })) {
            client.getElectionClient().observe(TestUtil.bytesOf("election"), listener(response -> {
                assertThat(Context.isOnWorkerThread()).isTrue();
                String proposal = response.getKv().getValue().toString(StandardCharsets.UTF_8);
                if (proposal.equals("first")) {
                    firstStarted.complete(null);
                    releaseFirst.orTimeout(10, TimeUnit.SECONDS).join();
                }
                proposals.add(proposal);
            }, error -> {
                terminalCalls.incrementAndGet();
                terminalOnWorker.complete(Context.isOnWorkerThread());
                proposalsAtEnd.complete(List.copyOf(proposals));
                end.complete(error);
            }));

            try {
                secondSent.get(5, TimeUnit.SECONDS);
                var get = client.getKVClient().get(TestUtil.bytesOf("key")).get(5, TimeUnit.SECONDS);
                assertThat(get.getHeader().getRevision()).isEqualTo(42);
                assertThat(proposals).isEmpty();
                assertThat(end).isNotDone();
            } finally {
                releaseFirst.complete(null);
            }

            Throwable error = end.get(5, TimeUnit.SECONDS);
            assertThat(terminalOnWorker.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(proposalsAtEnd.get(5, TimeUnit.SECONDS)).containsExactly("first", "second");
            assertThat(terminalCalls).hasValue(1);
            if (status == GrpcStatus.OK) {
                assertThat(error).isNull();
            } else {
                assertObserveFailure(error);
            }
        }
    }

    @Test
    void reportsServerFailureBeforeAnyProclamation() throws Exception {
        var end = new CompletableFuture<Throwable>();
        var proposals = new AtomicInteger();
        try (Client client = startServer(response -> response.status(GrpcStatus.UNAVAILABLE)
            .statusMessage(OBSERVE_FAILURE).end())) {
            client.getElectionClient().observe(TestUtil.bytesOf("election"),
                listener(response -> proposals.incrementAndGet(), end::complete));

            assertObserveFailure(end.get(5, TimeUnit.SECONDS));
            assertThat(proposals).hasValue(0);
        }
    }

    @Test
    void cancelsObservationWhenListenerThrows() throws Exception {
        var cancelSeen = new CompletableFuture<Throwable>();
        var end = new CompletableFuture<Throwable>();
        var proposals = new AtomicInteger();
        var terminalCalls = new AtomicInteger();
        var failure = new IllegalStateException("Election observer failed");

        try (Client client = startServer(response -> {
            response.exceptionHandler(cancelSeen::complete);
            response.write(proclamation("first")).compose(ignored -> response.write(proclamation("second")));
        })) {
            client.getElectionClient().observe(TestUtil.bytesOf("election"), listener(response -> {
                proposals.incrementAndGet();
                throw failure;
            }, error -> {
                terminalCalls.incrementAndGet();
                end.complete(error);
            }));

            assertThat(end.get(5, TimeUnit.SECONDS)).isInstanceOfSatisfying(EtcdException.class,
                error -> assertThat(error.getCause()).isSameAs(failure));
            assertThat(cancelSeen.get(5, TimeUnit.SECONDS)).isInstanceOfSatisfying(GrpcErrorException.class,
                error -> assertThat(error.error()).isEqualTo(GrpcError.CANCELLED));
            assertThat(proposals).hasValue(1);
            assertThat(terminalCalls).hasValue(1);
        }
    }

    private static void assertObserveFailure(Throwable error) {
        assertThat(error).isInstanceOfSatisfying(EtcdException.class, failure -> {
            assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.UNAVAILABLE);
            assertThat(failure).hasMessage(OBSERVE_FAILURE);
            assertThat(failure.getCause()).isInstanceOfSatisfying(InvalidStatusException.class,
                original -> assertThat(original.actualStatus()).isEqualTo(GrpcStatus.UNAVAILABLE));
        });
    }

    private Client startServer(
        Handler<GrpcServerResponse<io.etcd.jetcd.api.LeaderRequest, io.etcd.jetcd.api.LeaderResponse>> onObserve)
        throws Exception {
        var observe = io.etcd.jetcd.api.ElectionGrpcClient.Observe;
        ServiceMethod<io.etcd.jetcd.api.LeaderRequest, io.etcd.jetcd.api.LeaderResponse> observeMethod = ServiceMethod.server(
            observe.serviceName(), observe.methodName(), observe.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.LeaderRequest.getDefaultInstance()));
        var range = io.etcd.jetcd.api.KVGrpcClient.Range;
        ServiceMethod<io.etcd.jetcd.api.RangeRequest, io.etcd.jetcd.api.RangeResponse> rangeMethod = ServiceMethod.server(
            range.serviceName(), range.methodName(), range.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.RangeRequest.getDefaultInstance()));
        var grpcServer = GrpcServer.server(vertx)
            .callHandler(observeMethod, request -> request.handler(message -> onObserve.handle(request.response())))
            .callHandler(rangeMethod, request -> request.handler(message -> request.response().end(
                io.etcd.jetcd.api.RangeResponse.newBuilder()
                    .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setRevision(42)).build())));
        var server = vertx.createHttpServer().requestHandler(grpcServer).listen(0, EtcdConstants.LOCALHOST)
            .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        return Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + server.actualPort()).vertx(vertx).build();
    }

    private static io.etcd.jetcd.api.LeaderResponse proclamation(String value) {
        return io.etcd.jetcd.api.LeaderResponse.newBuilder()
            .setKv(io.etcd.jetcd.api.KeyValue.newBuilder().setValue(ByteString.copyFromUtf8(value)))
            .build();
    }

    private static Election.Listener listener(Consumer<LeaderResponse> onNext, Consumer<Throwable> terminal) {
        return new Election.Listener() {
            @Override
            public void onNext(LeaderResponse response) {
                onNext.accept(response);
            }

            @Override
            public void onError(Throwable error) {
                terminal.accept(error);
            }

            @Override
            public void onCompleted() {
                terminal.accept(null);
            }
        };
    }
}
