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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(30)
class SnapshotStreamingTest {
    private static final String SNAPSHOT_FAILURE = "snapshot source is unavailable";

    private final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(1));

    @AfterEach
    void closeVertx() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    @Test
    void completesAfterFinalWriteWithoutBlockingKv() throws Exception {
        var finalWriteStarted = new CompletableFuture<Void>();
        var releaseFinalWrite = new CountDownLatch(1);
        var contents = new ByteArrayOutputStream();
        var writes = new AtomicInteger();
        OutputStream sink = new OutputStream() {
            @Override
            public void write(int value) {
                contents.write(value);
            }

            @Override
            public void write(byte[] data, int offset, int length) throws IOException {
                if (writes.incrementAndGet() == 2) {
                    finalWriteStarted.complete(null);
                    try {
                        if (!releaseFinalWrite.await(10, TimeUnit.SECONDS)) {
                            throw new IOException("Timed out waiting to release snapshot write");
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IOException(error);
                    }
                }
                contents.write(data, offset, length);
            }
        };

        try (Client client = startServer(
            response -> response.write(chunk("first ")).compose(v -> response.end(chunk("last"))))) {
            var snapshot = client.getMaintenanceClient().snapshot(sink);
            try {
                finalWriteStarted.get(5, TimeUnit.SECONDS);
                var get = client.getKVClient().get(TestUtil.bytesOf("key")).get(5, TimeUnit.SECONDS);
                assertThat(get.getHeader().getRevision()).isEqualTo(42);
                assertThat(snapshot).isNotDone();
            } finally {
                releaseFinalWrite.countDown();
            }
            assertThat(snapshot.get(5, TimeUnit.SECONDS)).isEqualTo(10L);
            assertThat(contents.toString(StandardCharsets.UTF_8)).isEqualTo("first last");
        }
    }

    @Test
    void cancelsSnapshotOnWriteFailure() throws Exception {
        var cancelSeen = new CompletableFuture<Throwable>();
        var writes = new AtomicInteger();
        var failure = new IOException("Snapshot sink failed");
        OutputStream sink = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                writes.incrementAndGet();
                throw failure;
            }
        };

        try (Client client = startServer(response -> {
            response.exceptionHandler(cancelSeen::complete);
            response.write(chunk("first")).compose(v -> response.write(chunk("next")));
        })) {
            var snapshot = client.getMaintenanceClient().snapshot(sink);
            assertThatThrownBy(() -> snapshot.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                .cause().isInstanceOfSatisfying(EtcdException.class, error -> {
                    assertThat(error).hasMessage(failure.getMessage());
                    assertThat(error.getCause()).isSameAs(failure);
                });
            assertThat(cancelSeen.get(5, TimeUnit.SECONDS)).isInstanceOfSatisfying(GrpcErrorException.class,
                error -> assertThat(error.error()).isEqualTo(GrpcError.CANCELLED));
            assertThat(writes).hasValue(1);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reportsServerFailureWithAnyPartialData(boolean partial) throws Exception {
        try (Client client = startServer(response -> {
            var data = partial ? response.write(chunk("partial")) : Future.<Void> succeededFuture();
            data.compose(v -> response.status(GrpcStatus.UNAVAILABLE).statusMessage(SNAPSHOT_FAILURE).end());
        })) {
            var sink = new ByteArrayOutputStream();
            var snapshot = client.getMaintenanceClient().snapshot(sink);
            assertThatThrownBy(() -> snapshot.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                .cause().satisfies(SnapshotStreamingTest::assertSnapshotFailure);
            assertThat(sink.toString(StandardCharsets.UTF_8)).isEqualTo(partial ? "partial" : "");
        }
    }

    @ParameterizedTest
    @EnumSource(names = { "OK", "UNAVAILABLE" })
    void listenerFinishesAfterFinalChunkWithoutBlockingKv(GrpcStatus status) throws Exception {
        var finalChunkStarted = new CompletableFuture<Void>();
        var releaseFinalChunk = new CompletableFuture<Void>();
        var terminal = new CompletableFuture<Throwable>();
        var terminalOnWorker = new CompletableFuture<Boolean>();
        var chunks = new ArrayList<String>();
        Consumer<Throwable> finish = error -> {
            terminalOnWorker.complete(Context.isOnWorkerThread());
            terminal.complete(error);
        };

        try (Client client = startServer(response -> response.write(chunk("first"))
            .compose(v -> response.write(chunk("last")))
            .compose(v -> {
                if (status == GrpcStatus.UNAVAILABLE) {
                    response.statusMessage(SNAPSHOT_FAILURE);
                }
                return response.status(status).end();
            }))) {
            client.getMaintenanceClient().snapshot(response -> {
                assertThat(Context.isOnWorkerThread()).isTrue();
                String chunk = response.getBlob().toStringUtf8();
                if (chunk.equals("last")) {
                    finalChunkStarted.complete(null);
                    releaseFinalChunk.orTimeout(10, TimeUnit.SECONDS).join();
                }
                chunks.add(chunk);
            }, finish, () -> finish.accept(null));

            try {
                finalChunkStarted.get(5, TimeUnit.SECONDS);
                var get = client.getKVClient().get(TestUtil.bytesOf("key")).get(5, TimeUnit.SECONDS);
                assertThat(get.getHeader().getRevision()).isEqualTo(42);
                assertThat(terminal).isNotDone();
            } finally {
                releaseFinalChunk.complete(null);
            }

            Throwable error = terminal.get(5, TimeUnit.SECONDS);
            assertThat(terminalOnWorker.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(chunks).containsExactly("first", "last");
            if (status == GrpcStatus.OK) {
                assertThat(error).isNull();
            } else {
                assertSnapshotFailure(error);
            }
        }
    }

    private static void assertSnapshotFailure(Throwable error) {
        assertThat(error).isInstanceOfSatisfying(EtcdException.class, failure -> {
            assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.UNAVAILABLE);
            assertThat(failure).hasMessage(SNAPSHOT_FAILURE);
            assertThat(failure.getCause()).isInstanceOfSatisfying(InvalidStatusException.class,
                original -> assertThat(original.actualStatus()).isEqualTo(GrpcStatus.UNAVAILABLE));
        });
    }

    @Test
    void cancelsSnapshotWhenListenerThrows() throws Exception {
        var cancelSeen = new CompletableFuture<Throwable>();
        var terminal = new CompletableFuture<Throwable>();
        var terminalCalls = new AtomicInteger();
        var chunks = new AtomicInteger();
        var failure = new IllegalStateException("Snapshot listener failed");
        Consumer<Throwable> finish = error -> {
            terminalCalls.incrementAndGet();
            terminal.complete(error);
        };

        try (Client client = startServer(response -> {
            response.exceptionHandler(cancelSeen::complete);
            response.write(chunk("first")).compose(v -> response.write(chunk("next")));
        })) {
            client.getMaintenanceClient().snapshot(response -> {
                chunks.incrementAndGet();
                throw failure;
            }, finish, () -> finish.accept(null));

            assertThat(terminal.get(5, TimeUnit.SECONDS)).isInstanceOfSatisfying(EtcdException.class,
                error -> assertThat(error.getCause()).isSameAs(failure));
            assertThat(cancelSeen.get(5, TimeUnit.SECONDS)).isInstanceOfSatisfying(GrpcErrorException.class,
                error -> assertThat(error.error()).isEqualTo(GrpcError.CANCELLED));
            assertThat(chunks).hasValue(1);
            assertThat(terminalCalls).hasValue(1);
        }
    }

    private Client startServer(
        Handler<GrpcServerResponse<io.etcd.jetcd.api.SnapshotRequest, io.etcd.jetcd.api.SnapshotResponse>> snapshotHandler)
        throws Exception {
        var snapshot = io.etcd.jetcd.api.MaintenanceGrpcClient.Snapshot;
        ServiceMethod<io.etcd.jetcd.api.SnapshotRequest, io.etcd.jetcd.api.SnapshotResponse> snapshotMethod = ServiceMethod
            .server(
                snapshot.serviceName(), snapshot.methodName(), snapshot.cardinality(), GrpcMessageEncoder.encoder(),
                GrpcMessageDecoder.decoder(io.etcd.jetcd.api.SnapshotRequest.getDefaultInstance()));
        var range = io.etcd.jetcd.api.KVGrpcClient.Range;
        ServiceMethod<io.etcd.jetcd.api.RangeRequest, io.etcd.jetcd.api.RangeResponse> rangeMethod = ServiceMethod.server(
            range.serviceName(), range.methodName(), range.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.RangeRequest.getDefaultInstance()));
        var grpcServer = GrpcServer.server(vertx)
            .callHandler(snapshotMethod, request -> request.handler(message -> snapshotHandler.handle(request.response())))
            .callHandler(rangeMethod, request -> request.handler(message -> request.response().end(
                io.etcd.jetcd.api.RangeResponse.newBuilder()
                    .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setRevision(42)).build())));
        var server = vertx.createHttpServer().requestHandler(grpcServer).listen(0, EtcdConstants.LOCALHOST)
            .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        return Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + server.actualPort()).vertx(vertx).build();
    }

    private static io.etcd.jetcd.api.SnapshotResponse chunk(String data) {
        return io.etcd.jetcd.api.SnapshotResponse.newBuilder().setBlob(ByteString.copyFromUtf8(data)).build();
    }
}
