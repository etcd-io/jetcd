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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.SecureByteSequence;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.test.EtcdConstants;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.grpc.server.GrpcServerResponse;

import com.google.protobuf.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(20)
class LeaseGrantTest {
    private Vertx vertx;
    private GrpcServer grpcServer;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        grpcServer = GrpcServer.server(vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        await(vertx.close());
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "true, false", "false, true", "true, true" })
    void returnsAnAmbiguousGrantFailureWithoutAllocatingAnotherLease(boolean timed, boolean disconnect) throws Exception {
        var allocations = new AtomicInteger();
        handleGrant(request -> request.handler(message -> {
            int id = allocations.incrementAndGet();
            if (id > 1) {
                replyWithLease(request, id);
            } else if (disconnect) {
                request.connection().close();
            } else {
                request.response().status(GrpcStatus.UNAVAILABLE).end();
            }
        }));

        try (Client client = listen().build()) {
            var lease = client.getLeaseClient();
            var grant = timed ? lease.grant(10, 5, TimeUnit.SECONDS) : lease.grant(10);
            var failure = assertThatThrownBy(() -> grant.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
            if (!disconnect) {
                failure.cause().isInstanceOfSatisfying(InvalidStatusException.class,
                    error -> assertThat(error.actualStatus()).isEqualTo(GrpcStatus.UNAVAILABLE));
            }
            assertThat(allocations).hasValue(1);
        }
    }

    @Test
    void sendsTheDeadlineAndCancelsTheGrantOnExpiry() throws Exception {
        var received = new CompletableFuture<Long>();
        var cancelled = new CompletableFuture<GrpcError>();
        handleGrant(request -> request.errorHandler(cancelled::complete)
            .handler(message -> received.complete(request.timeout())));

        try (Client client = listen().build()) {
            var grant = client.getLeaseClient().grant(10, 2, TimeUnit.SECONDS);
            assertThat(received.get(5, TimeUnit.SECONDS)).isPositive();
            assertDeadlineExceeded(grant);
            assertThat(cancelled.get(5, TimeUnit.SECONDS)).isEqualTo(GrpcError.CANCELLED);
        }
    }

    @Test
    void deadlineIncludesAuthRetryBackoff() throws Exception {
        var authentications = new AtomicInteger();
        var firstGrant = new CompletableFuture<Void>();
        handleAuthentication(request -> request.handler(message -> request.response().end(
            io.etcd.jetcd.api.AuthenticateResponse.newBuilder().setToken("token-" + authentications.incrementAndGet())
                .build())));
        handleGrant(request -> request.handler(message -> request.response().status(GrpcStatus.UNAUTHENTICATED)
            .statusMessage(Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE).end()
            .onSuccess(firstGrant::complete).onFailure(firstGrant::completeExceptionally)));

        try (var user = SecureByteSequence.from("user");
            var password = SecureByteSequence.from("password");
            Client client = listen().user(user).password(password).retryDelay(10_000).retryMaxDelay(20_000).build()) {
            var grant = client.getLeaseClient().grant(10, 2, TimeUnit.SECONDS);
            firstGrant.get(5, TimeUnit.SECONDS);
            assertDeadlineExceeded(grant);
            assertThat(authentications).hasValue(1);
        }
    }

    @Test
    void deadlineIncludesWaitingForAuthentication() throws Exception {
        var authentication = new CompletableFuture<GrpcServerResponse<io.etcd.jetcd.api.AuthenticateRequest, io.etcd.jetcd.api.AuthenticateResponse>>();
        var ttls = new ConcurrentLinkedQueue<Long>();
        handleAuthentication(request -> request.handler(message -> authentication.complete(request.response())));
        handleGrant(request -> request.handler(message -> {
            ttls.add(message.getTTL());
            replyWithLease(request, 1);
        }));

        try (var user = SecureByteSequence.from("user");
            var password = SecureByteSequence.from("password");
            Client client = listen().user(user).password(password).build()) {
            var lease = client.getLeaseClient();
            var grant = lease.grant(10, 2, TimeUnit.SECONDS);
            var response = authentication.get(5, TimeUnit.SECONDS);
            assertDeadlineExceeded(grant);

            var nextGrant = lease.grant(20);
            await(response.end(io.etcd.jetcd.api.AuthenticateResponse.newBuilder().setToken("token").build()));
            assertThat(nextGrant.get(5, TimeUnit.SECONDS).getID()).isEqualTo(1);
            assertThat(ttls).containsExactly(20L);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void retriesGrantWhenEtcdRejectedTheToken(boolean timed) throws Exception {
        var authentications = new AtomicInteger();
        var requests = new AtomicInteger();
        handleAuthentication(request -> request.handler(message -> request.response().end(
            io.etcd.jetcd.api.AuthenticateResponse.newBuilder().setToken("token-" + authentications.incrementAndGet())
                .build())));
        handleGrant(request -> request.handler(message -> {
            if (requests.incrementAndGet() == 1) {
                request.response().status(GrpcStatus.UNAUTHENTICATED)
                    .statusMessage(Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE).end();
            } else {
                replyWithLease(request, 1);
            }
        }));

        try (var user = SecureByteSequence.from("user");
            var password = SecureByteSequence.from("password");
            Client client = listen().user(user).password(password).build()) {
            var lease = client.getLeaseClient();
            var grant = timed ? lease.grant(10, 5, TimeUnit.SECONDS) : lease.grant(10);
            assertThat(grant.get(5, TimeUnit.SECONDS).getID()).isEqualTo(1);
            assertThat(authentications).hasValue(2);
            assertThat(requests).hasValue(2);
        }
    }

    @Test
    void rejectsAnAlreadyExpiredDeadline() throws Exception {
        var requests = new AtomicInteger();
        handleGrant(request -> request.handler(message -> {
            requests.incrementAndGet();
            replyWithLease(request, 1);
        }));

        try (Client client = listen().build()) {
            assertDeadlineExceeded(client.getLeaseClient().grant(10, 0, TimeUnit.SECONDS));
            assertThat(requests).hasValue(0);
        }
    }

    private ClientBuilder listen() throws Exception {
        var http = await(vertx.createHttpServer().requestHandler(grpcServer).listen(0, EtcdConstants.LOCALHOST));
        return Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + http.actualPort())
            .vertx(vertx).retryMaxAttempts(1);
    }

    private void handleGrant(
        Handler<GrpcServerRequest<io.etcd.jetcd.api.LeaseGrantRequest, io.etcd.jetcd.api.LeaseGrantResponse>> handler) {
        handle(io.etcd.jetcd.api.LeaseGrpcClient.LeaseGrant, io.etcd.jetcd.api.LeaseGrantRequest.getDefaultInstance(), handler);
    }

    private void handleAuthentication(
        Handler<GrpcServerRequest<io.etcd.jetcd.api.AuthenticateRequest, io.etcd.jetcd.api.AuthenticateResponse>> handler) {
        handle(io.etcd.jetcd.api.AuthGrpcClient.Authenticate, io.etcd.jetcd.api.AuthenticateRequest.getDefaultInstance(),
            handler);
    }

    private <Req extends Message, Resp extends Message> void handle(ServiceMethod<Resp, Req> clientMethod, Req prototype,
        Handler<GrpcServerRequest<Req, Resp>> handler) {
        var method = ServiceMethod.<Req, Resp> server(clientMethod.serviceName(), clientMethod.methodName(),
            clientMethod.cardinality(), GrpcMessageEncoder.encoder(), GrpcMessageDecoder.decoder(prototype));
        grpcServer.callHandler(method, handler);
    }

    private static void replyWithLease(
        GrpcServerRequest<io.etcd.jetcd.api.LeaseGrantRequest, io.etcd.jetcd.api.LeaseGrantResponse> request, int id) {
        request.response().end(io.etcd.jetcd.api.LeaseGrantResponse.newBuilder().setID(id).build());
    }

    private static void assertDeadlineExceeded(CompletableFuture<?> future) {
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
            .cause().isInstanceOfSatisfying(EtcdException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED));
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
}
