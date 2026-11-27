package io.etcd.jetcd.impl;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.test.EtcdConstants;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpConnection;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;

import com.google.protobuf.ByteString;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class RetryTest {
    @Tag("kv")
    @ParameterizedTest
    @CsvSource({
            "UNAVAILABLE, true, true",
            "UNAVAILABLE, false, false",
            "UNKNOWN, false, false",
            "INVALID_ARGUMENT, true, false"
    })
    void retriesPutOnlyForAnEligibleFailure(GrpcStatus status, boolean autoRetry, boolean expectRetry) throws Exception {
        var vertx = Vertx.vertx();
        var requests = new ConcurrentLinkedQueue<io.etcd.jetcd.api.PutRequest>();
        var put = io.etcd.jetcd.api.KVGrpcClient.Put;
        ServiceMethod<io.etcd.jetcd.api.PutRequest, io.etcd.jetcd.api.PutResponse> method = ServiceMethod.server(
            put.serviceName(), put.methodName(), put.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.PutRequest.getDefaultInstance()));

        try {
            var grpcServer = GrpcServer.server(vertx).callHandler(method, request -> request.handler(message -> {
                requests.add(message);
                if (requests.size() == 1) {
                    request.response().status(status).end();
                } else {
                    request.response().end(io.etcd.jetcd.api.PutResponse.newBuilder()
                        .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setRevision(1)).build());
                }
            }));
            var server = vertx.createHttpServer().requestHandler(grpcServer).listen(0, EtcdConstants.LOCALHOST)
                .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            var endpoint = "http://" + EtcdConstants.LOCALHOST + ":" + server.actualPort();
            var expected = io.etcd.jetcd.api.PutRequest.newBuilder()
                .setKey(ByteString.copyFromUtf8("key")).setValue(ByteString.copyFromUtf8("value")).build();
            var option = autoRetry ? PutOption.builder().withAutoRetry().build() : PutOption.DEFAULT;

            try (Client client = Client.builder(endpoint).retryMaxAttempts(1).build()) {
                var result = client.getKVClient().put(bytesOf("key"), bytesOf("value"), option);
                if (expectRetry) {
                    assertThat(result.get(5, TimeUnit.SECONDS).getHeader().getRevision()).isEqualTo(1);
                    assertThat(requests).containsExactly(expected, expected);
                } else {
                    assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                        .cause().isInstanceOfSatisfying(InvalidStatusException.class,
                            error -> assertThat(error.actualStatus()).isEqualTo(status));
                    assertThat(requests).containsExactly(expected);
                }
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void failsOverFromDisconnectedMember() throws Exception {
        var vertx = Vertx.vertx();
        var rejectedConnections = new AtomicInteger();
        var range = io.etcd.jetcd.api.KVGrpcClient.Range;
        ServiceMethod<io.etcd.jetcd.api.RangeRequest, io.etcd.jetcd.api.RangeResponse> method = ServiceMethod.server(
            range.serviceName(), range.methodName(), range.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.RangeRequest.getDefaultInstance()));

        try {
            var disconnected = vertx.createNetServer().connectHandler(socket -> {
                rejectedConnections.incrementAndGet();
                var unused = socket.close();
            }).listen(0, EtcdConstants.LOCALHOST).toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            var response = io.etcd.jetcd.api.RangeResponse.newBuilder()
                .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setMemberId(42)).build();
            var grpcServer = GrpcServer.server(vertx).callHandler(method,
                request -> request.handler(message -> request.response().end(response)));
            var healthy = vertx.createHttpServer().requestHandler(grpcServer).listen(0, EtcdConstants.LOCALHOST)
                .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            String baseUrl = "http://" + EtcdConstants.LOCALHOST + ":";
            try (Client client = Client.builder(baseUrl + disconnected.actualPort(), baseUrl + healthy.actualPort())
                .vertx(vertx).connectTimeout(Duration.ofSeconds(2)).retryDelay(10).retryMaxAttempts(2).build()) {
                var result = client.getKVClient().get(bytesOf("sample_key")).get(5, TimeUnit.SECONDS);
                assertThat(result.getHeader().getMemberId()).isEqualTo(42);
                assertThat(rejectedConnections).hasValueGreaterThan(0);
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "127.0.0.1", "::1" })
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void retriesOnNewConnectionAfterDisconnect(String host) throws Exception {
        var vertx = Vertx.vertx();
        var currentConnection = new AtomicReference<HttpConnection>();
        var calls = new AtomicInteger();
        var range = io.etcd.jetcd.api.KVGrpcClient.Range;
        ServiceMethod<io.etcd.jetcd.api.RangeRequest, io.etcd.jetcd.api.RangeResponse> method = ServiceMethod.server(
            range.serviceName(), range.methodName(), range.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.RangeRequest.getDefaultInstance()));

        try {
            var grpcServer = GrpcServer.server(vertx).callHandler(method, request -> {
                request.handler(message -> {
                    if (calls.incrementAndGet() == 2) {
                        request.response().status(GrpcStatus.UNAVAILABLE).end();
                    } else {
                        request.response().end(io.etcd.jetcd.api.RangeResponse.getDefaultInstance());
                    }
                });
            });
            var server = vertx.createHttpServer()
                .connectionHandler(currentConnection::set)
                .requestHandler(grpcServer)
                .listen(0, host)
                .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            var endpoint = new URI("http", null, host, server.actualPort(), null, null, null);

            try (Client client = Client.builder(endpoint.toString()).retryMaxAttempts(2).build()) {
                var kv = client.getKVClient();
                var key = bytesOf("key");
                kv.get(key).get(5, TimeUnit.SECONDS);
                var firstConnection = currentConnection.get();
                firstConnection.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

                kv.get(key).get(5, TimeUnit.SECONDS);

                assertThat(currentConnection.get()).isNotSameAs(firstConnection);
                assertThat(calls).hasValue(3);
                var firstClose = client.closeAsync();
                var secondClose = client.closeAsync();
                CompletableFuture.allOf(firstClose, secondClose).get(5, TimeUnit.SECONDS);
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }
}
