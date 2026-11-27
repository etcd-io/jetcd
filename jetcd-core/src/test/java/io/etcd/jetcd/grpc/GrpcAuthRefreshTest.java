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

package io.etcd.jetcd.grpc;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.SecureByteSequence;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.test.EtcdConstants;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerResponse;

import com.google.protobuf.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(30)
class GrpcAuthRefreshTest {
    private static final ByteSequence KEY = ByteSequence.from("key", StandardCharsets.UTF_8);
    private final BlockingQueue<Call<io.etcd.jetcd.api.AuthenticateRequest, io.etcd.jetcd.api.AuthenticateResponse>> authCalls = new LinkedBlockingQueue<>();
    private final BlockingQueue<Call<io.etcd.jetcd.api.RangeRequest, io.etcd.jetcd.api.RangeResponse>> rangeCalls = new LinkedBlockingQueue<>();
    private final BlockingQueue<Call<io.etcd.jetcd.api.WatchRequest, io.etcd.jetcd.api.WatchResponse>> watchCalls = new LinkedBlockingQueue<>();
    private final AtomicInteger authenticationCount = new AtomicInteger();
    private final SecureByteSequence user = SecureByteSequence.from("user");
    private final SecureByteSequence password = SecureByteSequence.from("password");

    private Vertx vertx;
    private ClientBuilder builder;

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        var server = GrpcServer.server(vertx);
        capture(server, io.etcd.jetcd.api.AuthGrpcClient.Authenticate,
            io.etcd.jetcd.api.AuthenticateRequest.getDefaultInstance(),
            authCalls, authenticationCount::incrementAndGet);
        capture(server, io.etcd.jetcd.api.KVGrpcClient.Range, io.etcd.jetcd.api.RangeRequest.getDefaultInstance(),
            rangeCalls, () -> {
            });
        capture(server, io.etcd.jetcd.api.WatchGrpcClient.Watch, io.etcd.jetcd.api.WatchRequest.getDefaultInstance(),
            watchCalls, () -> {
            });

        var http = await(vertx.createHttpServer().requestHandler(server).listen(0, EtcdConstants.LOCALHOST));
        builder = Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + http.actualPort())
            .user(user).password(password).vertx(vertx).retryMaxAttempts(1);
    }

    @AfterEach
    void tearDown() throws Exception {
        user.close();
        password.close();
        await(vertx.close());
    }

    @Test
    void sharesAuthenticationAndCanRetryAfterFailure() throws Exception {
        var service = new GrpcService(builder);
        try {
            var auth = service.auth();
            var first = auth.getToken();
            var pending = next(authCalls);
            var second = auth.getToken();
            var cancelled = auth.getToken();
            assertThat(cancelled.cancel(false)).isTrue();

            await(pending.response().status(GrpcStatus.UNAUTHENTICATED).end());
            assertThatThrownBy(() -> first.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(InvalidStatusException.class);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(InvalidStatusException.class);

            var retry = auth.getToken();
            pending = next(authCalls);
            var joining = auth.getToken();
            replyWithToken(pending, "replacement");
            assertThat(retry).succeedsWithin(5, TimeUnit.SECONDS).isEqualTo("replacement");
            assertThat(joining).succeedsWithin(5, TimeUnit.SECONDS).isEqualTo("replacement");
            assertThat(authenticationCount).hasValue(2);
        } finally {
            service.close().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void closeFailsPendingWaitersAndRejectsLateAuthentication() throws Exception {
        var service = new GrpcService(builder);
        try {
            var auth = service.auth();
            var first = auth.getToken();
            var pending = next(authCalls);
            var second = auth.getToken();

            auth.close();
            assertThatThrownBy(() -> first.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(ClosedClientException.class);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(ClosedClientException.class);
            replyWithToken(pending, "late");
            assertThatThrownBy(() -> auth.getToken().get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(ClosedClientException.class);
            assertThat(authenticationCount).hasValue(1);
        } finally {
            service.close().get(5, TimeUnit.SECONDS);
        }
    }

    @ParameterizedTest
    @EnumSource(value = GrpcStatus.class, names = { "UNAUTHENTICATED", "INVALID_ARGUMENT" })
    void ignoresRejectionOfAnEarlierAuthenticationEvenWhenEtcdReissuesTheSameToken(GrpcStatus status) throws Exception {
        try (Client client = builder.build()) {
            var kv = client.getKVClient();
            var first = kv.get(KEY);
            replyWithToken(next(authCalls), "same-token");
            var firstCall = next(rangeCalls);
            var delayed = kv.get(KEY);
            var delayedCall = next(rangeCalls);
            assertThat(firstCall.token()).isEqualTo("same-token");
            assertThat(delayedCall.token()).isEqualTo("same-token");

            await(firstCall.response().status(status).statusMessage(authMessage(status)).end());
            replyWithToken(next(authCalls), "same-token");
            assertThat(replyToRange().token()).isEqualTo("same-token");
            first.get(5, TimeUnit.SECONDS);

            await(delayedCall.response().status(status).statusMessage(authMessage(status)).end());
            assertThat(replyToRange().token()).isEqualTo("same-token");
            delayed.get(5, TimeUnit.SECONDS);
            assertThat(authenticationCount).hasValue(2);
        }
    }

    @Test
    void keepsTheTokenAfterAnOrdinaryInvalidArgument() throws Exception {
        try (Client client = builder.build()) {
            var kv = client.getKVClient();
            var rejected = kv.get(KEY);
            replyWithToken(next(authCalls), "original");
            var call = next(rangeCalls);
            await(call.response().status(GrpcStatus.INVALID_ARGUMENT).statusMessage("etcdserver: key is not provided").end());
            assertThatThrownBy(() -> rejected.get(5, TimeUnit.SECONDS))
                .cause().isInstanceOfSatisfying(InvalidStatusException.class,
                    error -> assertThat(error.actualStatus()).isEqualTo(GrpcStatus.INVALID_ARGUMENT));

            var next = kv.get(KEY);
            assertThat(replyToRange().token()).isEqualTo("original");
            next.get(5, TimeUnit.SECONDS);
            assertThat(authenticationCount).hasValue(1);
        }
    }

    @Test
    void watchReconnectOnlyInvalidatesItsOwnAuthentication() throws Exception {
        try (Client client = builder.build();
            var watcher = client.getWatchClient().watch(KEY, response -> {
            })) {
            replyWithToken(next(authCalls), "original");
            var watch = next(watchCalls);
            assertThat(watch.token()).isEqualTo("original");

            var get = client.getKVClient().get(KEY);
            var range = next(rangeCalls);
            assertThat(range.token()).isEqualTo("original");
            await(range.response().status(GrpcStatus.UNAUTHENTICATED)
                .statusMessage(Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE).end());
            replyWithToken(next(authCalls), "replacement");
            assertThat(replyToRange().token()).isEqualTo("replacement");
            get.get(5, TimeUnit.SECONDS);

            var canceled = io.etcd.jetcd.api.WatchResponse.newBuilder()
                .setCreated(true).setCanceled(true).setCancelReason(Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE).build();
            await(watch.response().write(canceled));
            var reconnected = next(watchCalls);
            assertThat(reconnected.token()).isEqualTo("replacement");

            await(reconnected.response().write(canceled));
            replyWithToken(next(authCalls), "refreshed-by-watch");
            assertThat(next(watchCalls).token()).isEqualTo("refreshed-by-watch");
            assertThat(watcher.isClosed()).isFalse();
            assertThat(authenticationCount).hasValue(3);
        }
    }

    private static String authMessage(GrpcStatus status) {
        return status == GrpcStatus.UNAUTHENTICATED ? Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE : Errors.ERROR_AUTH_STORE_OLD;
    }

    private Call<io.etcd.jetcd.api.RangeRequest, io.etcd.jetcd.api.RangeResponse> replyToRange() throws Exception {
        var call = next(rangeCalls);
        await(call.response().end(io.etcd.jetcd.api.RangeResponse.getDefaultInstance()));
        return call;
    }

    private static void replyWithToken(Call<io.etcd.jetcd.api.AuthenticateRequest, io.etcd.jetcd.api.AuthenticateResponse> call,
        String token) throws Exception {
        await(call.response().end(io.etcd.jetcd.api.AuthenticateResponse.newBuilder().setToken(token).build()));
    }

    private static <Req extends Message, Resp extends Message> void capture(GrpcServer server,
        ServiceMethod<Resp, Req> clientMethod, Req prototype, BlockingQueue<Call<Req, Resp>> calls, Runnable onCall) {
        var method = ServiceMethod.<Req, Resp> server(clientMethod.serviceName(), clientMethod.methodName(),
            clientMethod.cardinality(), GrpcMessageEncoder.encoder(), GrpcMessageDecoder.decoder(prototype));
        server.callHandler(method, request -> request.handler(message -> {
            onCall.run();
            calls.add(new Call<>(request.headers().get(GrpcAuth.TOKEN_HEADER), request.response()));
        }));
    }

    private static <T> T next(BlockingQueue<T> calls) throws InterruptedException {
        T call = calls.poll(5, TimeUnit.SECONDS);
        assertThat(call).as("observed gRPC request").isNotNull();
        return call;
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private record Call<Req, Resp>(String token, GrpcServerResponse<Req, Resp> response) {
    }
}
