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

import java.net.URI;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.SecureByteSequence;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.grpc.GrpcAuth;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

import com.google.protobuf.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class MaintenanceMemberRoutingTest {
    private static final String LOOPBACK = "127.0.0.1";
    private static final String HEADER = "x-test-tenant";

    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        await(vertx.close());
    }

    @ParameterizedTest
    @ValueSource(strings = { LOOPBACK, "::1" })
    void sendsEachMemberCallToTheTargetWithAuthAndHeaders(String host) throws Exception {
        var discovered = GrpcServer.server(vertx);
        var authentications = new AtomicInteger();
        handle(discovered, io.etcd.jetcd.api.AuthGrpcClient.Authenticate,
            io.etcd.jetcd.api.AuthenticateRequest.getDefaultInstance(),
            request -> request.handler(message -> {
                authentications.incrementAndGet();
                request.response()
                    .end(io.etcd.jetcd.api.AuthenticateResponse.newBuilder().setToken("token-from-cluster").build());
            }));

        var selected = GrpcServer.server(vertx);
        Queue<RequestHeaders> receivedHeaders = new ConcurrentLinkedQueue<>();
        var receivedRevision = new AtomicLong();
        var header = io.etcd.jetcd.api.ResponseHeader.newBuilder().setMemberId(42).build();
        handle(selected, io.etcd.jetcd.api.MaintenanceGrpcClient.Defragment,
            io.etcd.jetcd.api.DefragmentRequest.getDefaultInstance(),
            request -> request.handler(message -> {
                receivedHeaders.add(RequestHeaders.from(request));
                request.response().end(io.etcd.jetcd.api.DefragmentResponse.newBuilder().setHeader(header).build());
            }));
        handle(selected, io.etcd.jetcd.api.MaintenanceGrpcClient.Status, io.etcd.jetcd.api.StatusRequest.getDefaultInstance(),
            request -> request.handler(message -> {
                receivedHeaders.add(RequestHeaders.from(request));
                request.response().end(io.etcd.jetcd.api.StatusResponse.newBuilder().setHeader(header).build());
            }));
        handle(selected, io.etcd.jetcd.api.MaintenanceGrpcClient.Hash, io.etcd.jetcd.api.HashRequest.getDefaultInstance(),
            request -> request.handler(message -> {
                receivedHeaders.add(RequestHeaders.from(request));
                request.response().end(io.etcd.jetcd.api.HashResponse.newBuilder().setHeader(header).build());
            }));
        handle(selected, io.etcd.jetcd.api.MaintenanceGrpcClient.HashKV, io.etcd.jetcd.api.HashKVRequest.getDefaultInstance(),
            request -> request.handler(message -> {
                receivedHeaders.add(RequestHeaders.from(request));
                receivedRevision.set(message.getRevision());
                request.response().end(io.etcd.jetcd.api.HashKVResponse.newBuilder().setHeader(header).build());
            }));

        URI discoveryEndpoint = listen(discovered, new HttpServerOptions(), LOOPBACK);
        URI target = listen(selected, new HttpServerOptions(), host);
        try (var user = SecureByteSequence.from("test-user");
            var password = SecureByteSequence.from("test-password");
            Client client = Client.builder(discoveryEndpoint.toString())
                .user(user).password(password).header(HEADER, "tenant-a").build()) {
            var maintenance = client.getMaintenanceClient();
            assertThat(maintenance.defragmentMember(target.toString()).get().getHeader().getMemberId()).isEqualTo(42);
            assertThat(maintenance.statusMember(target.toString()).get().getHeader().getMemberId()).isEqualTo(42);
            assertThat(maintenance.hash(target.getRawAuthority()).get().getHeader().getMemberId()).isEqualTo(42);
            assertThat(maintenance.hashKV(target.getRawAuthority(), 1234).get().getHeader().getMemberId()).isEqualTo(42);
            assertThat(receivedRevision).hasValue(1234);
            assertThat(authentications).hasValue(1);
            var expected = new RequestHeaders("token-from-cluster", "tenant-a");
            assertThat(receivedHeaders).containsExactly(expected, expected, expected, expected);
        }
    }

    @Test
    void unavailableMemberIsNeverRetriedOrReplaced() throws Exception {
        var discoveredCalls = new AtomicInteger();
        var selectedCalls = new AtomicInteger();
        URI discovered = defragmentEndpoint(discoveredCalls, GrpcStatus.OK);
        URI target = defragmentEndpoint(selectedCalls, GrpcStatus.UNAVAILABLE);

        try (Client client = Client.builder(discovered.toString()).retryMaxAttempts(3).build()) {
            assertThatThrownBy(() -> client.getMaintenanceClient().defragmentMember(target.toString()).get())
                .isInstanceOf(ExecutionException.class)
                .cause().isInstanceOfSatisfying(EtcdException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAVAILABLE));
            assertThat(selectedCalls).hasValue(1);
            assertThat(discoveredCalls).hasValue(0);
        }
    }

    @Test
    void validatesTargetsBeforeSendingRequests() throws Exception {
        var calls = new AtomicInteger();
        URI target = defragmentEndpoint(calls, GrpcStatus.OK);
        try (Client client = Client.builder(target.toString()).build()) {
            var maintenance = client.getMaintenanceClient();
            String[] invalidTargets = {
                    null, "", " ", "http://", "http://missing-port:", "localhost:0", "localhost:65536", "localhost:-1",
                    "ftp://" + target.getRawAuthority(), "https://" + target.getRawAuthority(), target + "/path",
                    target + "/?query", target + "#fragment", "http://user@" + target.getRawAuthority()
            };
            for (String invalid : invalidTargets) {
                assertThatThrownBy(() -> maintenance.defragmentMember(invalid).get())
                    .as("Invalid target %s", invalid)
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(IllegalArgumentException.class);
            }
            assertThat(calls).hasValue(0);
        }
    }

    @Test
    void usesConfiguredTlsAndClosesDirectTransportWithoutClosingSharedVertx() throws Exception {
        var trustOptions = new PemTrustOptions().addCertPath("ssl/cert/ca.pem");
        var serverOptions = new HttpServerOptions().setSsl(true).setUseAlpn(true)
            .setClientAuth(ClientAuth.REQUIRED).setTrustOptions(trustOptions)
            .setKeyCertOptions(new PemKeyCertOptions()
                .setCertPath("ssl/cert/server.pem").setKeyPath("ssl/cert/server-key.pem"));
        var clientOptions = new HttpClientOptions().setSsl(true).setUseAlpn(true).setVerifyHost(false)
            .setTrustOptions(trustOptions)
            .setKeyCertOptions(new PemKeyCertOptions()
                .setCertPath("ssl/cert/client.pem").setKeyPath("ssl/cert/client-key.pem"));
        var grpcServer = GrpcServer.server(vertx);
        var calls = new AtomicInteger();
        handle(grpcServer, io.etcd.jetcd.api.MaintenanceGrpcClient.Status, io.etcd.jetcd.api.StatusRequest.getDefaultInstance(),
            request -> request.handler(message -> {
                calls.incrementAndGet();
                request.response().end(io.etcd.jetcd.api.StatusResponse.getDefaultInstance());
            }));
        var disconnected = new CompletableFuture<Void>();
        var server = await(vertx.createHttpServer(serverOptions)
            .connectionHandler(connection -> connection.closeHandler(v -> disconnected.complete(null)))
            .requestHandler(grpcServer).listen(0, LOOPBACK));
        URI target = new URI("https", null, LOOPBACK, server.actualPort(), null, null, null);

        try (Client client = Client.builder(target.toString()).httpClientOptions(clientOptions)
            .connectTimeout(Duration.ofSeconds(2)).vertx(vertx).build()) {
            var maintenance = client.getMaintenanceClient();
            assertThatThrownBy(() -> maintenance.statusMember("http://" + target.getRawAuthority()).get())
                .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(IllegalArgumentException.class);
            assertThat(calls).hasValue(0);
            maintenance.statusMember(target.toString()).get();
            maintenance.statusMember(target.getRawAuthority()).get();
            var closing = client.closeAsync();
            assertThatThrownBy(() -> maintenance.statusMember(target.toString()).get())
                .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(ClosedClientException.class);
            closing.get(5, TimeUnit.SECONDS);
            disconnected.get(5, TimeUnit.SECONDS);
            assertThat(calls).hasValue(2);

            try (Client another = Client.builder(target.toString()).httpClientOptions(clientOptions).vertx(vertx).build()) {
                another.getMaintenanceClient().statusMember(target.toString()).get();
                assertThat(calls).hasValue(3);
            }
        }
    }

    private URI defragmentEndpoint(AtomicInteger calls, GrpcStatus status) throws Exception {
        var server = GrpcServer.server(vertx);
        handle(server, io.etcd.jetcd.api.MaintenanceGrpcClient.Defragment,
            io.etcd.jetcd.api.DefragmentRequest.getDefaultInstance(),
            request -> request.handler(message -> {
                calls.incrementAndGet();
                if (status == GrpcStatus.OK) {
                    request.response().end(io.etcd.jetcd.api.DefragmentResponse.getDefaultInstance());
                } else {
                    request.response().status(status).end();
                }
            }));
        return listen(server, new HttpServerOptions(), LOOPBACK);
    }

    private URI listen(GrpcServer grpcServer, HttpServerOptions options, String host) throws Exception {
        var server = await(vertx.createHttpServer(options).requestHandler(grpcServer).listen(0, host));
        return new URI(options.isSsl() ? "https" : "http", null, host, server.actualPort(), null, null, null);
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static <Req extends Message, Resp extends Message> void handle(GrpcServer server,
        ServiceMethod<Resp, Req> clientMethod,
        Req prototype,
        Handler<GrpcServerRequest<Req, Resp>> handler) {
        ServiceMethod<Req, Resp> method = ServiceMethod.server(
            clientMethod.serviceName(), clientMethod.methodName(), clientMethod.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(prototype));
        server.callHandler(method, handler);
    }

    private record RequestHeaders(String token, String tenant) {
        static RequestHeaders from(GrpcServerRequest<?, ?> request) {
            return new RequestHeaders(request.headers().get(GrpcAuth.TOKEN_HEADER), request.headers().get(HEADER));
        }
    }
}
