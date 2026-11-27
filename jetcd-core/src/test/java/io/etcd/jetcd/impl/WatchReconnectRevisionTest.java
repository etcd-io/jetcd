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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.test.EtcdConstants;
import io.etcd.jetcd.watch.WatchResponse;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.MessageSizeOverflowException;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;

import com.google.protobuf.ByteString;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("watch")
@Timeout(30)
class WatchReconnectRevisionTest {
    @ParameterizedTest
    @CsvSource({
            "0, false, 10", // Live watch before its first event
            "5, false, 5", // Watch requested from an explicit revision
            "35, false, 35", // Watch requested ahead of the server's current revision
            "0, true, 12" // Live watch after delivering revision 11
    })
    void resumesAtTheSameRevisionWhenTheRecreatedStreamEndsBeforeReplay(long initialRevision,
        boolean sendInitialEvent, long expectedResumeRevision) throws Exception {

        var vertx = Vertx.vertx();
        BlockingQueue<Subscription> subscriptions = new LinkedBlockingQueue<>();
        BlockingQueue<WatchResponse> responses = new LinkedBlockingQueue<>();
        var key = TestUtil.bytesOf("key");
        var option = WatchOption.builder().withRevision(initialRevision).withCreateNotify(true).build();

        try (Client client = startServer(vertx, subscriptions).build();
            var watcher = client.getWatchClient().watch(key, option, responses::add)) {
            var initial = nextSubscription(subscriptions, initialRevision);
            acknowledge(initial, responses, 10);
            if (sendInitialEvent) {
                deliverEvent(initial, responses, key, 11);
            }
            await(initial.stream().end());

            var interruptedReplay = nextSubscription(subscriptions, expectedResumeRevision);
            acknowledge(interruptedReplay, responses, 30);
            await(interruptedReplay.stream().end());

            var resumed = nextSubscription(subscriptions, expectedResumeRevision);
            acknowledge(resumed, responses, 30);
            deliverEvent(resumed, responses, key, Math.max(12, expectedResumeRevision));
            assertThat(watcher.isClosed()).isFalse();
        } finally {
            await(vertx.close());
        }
    }

    @Test
    void replaysAnInterruptedFragmentedResponseAndDeliversItsEventsOnce() throws Exception {
        var vertx = Vertx.vertx();
        BlockingQueue<Subscription> subscriptions = new LinkedBlockingQueue<>();
        BlockingQueue<WatchResponse> responses = new LinkedBlockingQueue<>();
        var prefix = TestUtil.bytesOf("key/");
        var first = TestUtil.bytesOf("key/first");
        var second = TestUtil.bytesOf("key/second");
        var third = TestUtil.bytesOf("key/third");
        var option = WatchOption.builder().isPrefix(true).withRevision(12).withFragment(true).withCreateNotify(true).build();

        try (Client client = startServer(vertx, subscriptions).build();
            var watcher = client.getWatchClient().watch(prefix, option, responses::add)) {
            var initial = nextSubscription(subscriptions, 12);
            assertThat(initial.request().getFragment()).isTrue();
            acknowledge(initial, responses, 30);
            writeEvent(initial, first, 12, true);
            await(initial.stream().end());

            var resumed = nextSubscription(subscriptions, 12);
            acknowledge(resumed, responses, 30);
            writeEvent(resumed, first, 12, true);
            writeEvent(resumed, second, 12, true);
            writeEvent(resumed, third, 12, false);

            var response = responses.poll(5, TimeUnit.SECONDS);
            assertThat(response).as("assembled watch response").isNotNull();
            assertThat(response.getEvents()).extracting(event -> event.keyValue().getKey())
                .containsExactly(first, second, third);

            await(resumed.stream().end());
            acknowledge(nextSubscription(subscriptions, 13), responses, 30);
            assertThat(watcher.isClosed()).isFalse();
        } finally {
            await(vertx.close());
        }
    }

    @Test
    void rejectsWatchMessagesAboveTheConfiguredInboundSize() throws Exception {
        var vertx = Vertx.vertx();
        BlockingQueue<Subscription> subscriptions = new LinkedBlockingQueue<>();
        BlockingQueue<WatchResponse> responses = new LinkedBlockingQueue<>();
        var error = new CompletableFuture<Throwable>();
        var key = ByteSequence.from(new byte[128]);
        var option = WatchOption.builder().withCreateNotify(true).build();

        try (Client client = startServer(vertx, subscriptions).maxInboundMessageSize(64).build();
            var watcher = client.getWatchClient().watch(key, option, responses::add, error::complete)) {
            var subscription = nextSubscription(subscriptions, 0);
            acknowledge(subscription, responses, 10);
            writeEvent(subscription, key, 11, false);
            assertThat(error.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(MessageSizeOverflowException.class);
            assertThat(responses).isEmpty();
            watcher.closeAsync().get(5, TimeUnit.SECONDS);
        } finally {
            await(vertx.close());
        }
    }

    @Test
    void blockedListenerLeavesAnotherWatchAndKvResponsive() throws Exception {
        var vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(2));
        BlockingQueue<Subscription> subscriptions = new LinkedBlockingQueue<>();
        BlockingQueue<WatchResponse> slowResponses = new LinkedBlockingQueue<>();
        BlockingQueue<WatchResponse> otherResponses = new LinkedBlockingQueue<>();
        var slowKey = TestUtil.bytesOf("slow");
        var otherKey = TestUtil.bytesOf("other");
        var entered = new CompletableFuture<Boolean>();
        var release = new CompletableFuture<Void>();
        var option = WatchOption.builder().withCreateNotify(true).build();

        try (Client client = startServer(vertx, subscriptions).build();
            var slow = client.getWatchClient().watch(slowKey, option, response -> {
                if (!response.isCreatedNotify() && response.getHeader().getRevision() == 11) {
                    entered.complete(Context.isOnWorkerThread());
                    release.join();
                }
                slowResponses.add(response);
            })) {
            try {
                var initial = nextSubscription(subscriptions, 0);
                acknowledge(initial, slowResponses, 10);
                writeEvent(initial, slowKey, 11, false);
                assertThat(entered.get(5, TimeUnit.SECONDS)).isTrue();
                writeEvent(initial, slowKey, 12, false);

                try (var other = client.getWatchClient().watch(otherKey, option, otherResponses::add)) {
                    var otherSubscription = nextSubscription(subscriptions, 0);
                    acknowledge(otherSubscription, otherResponses, 10);
                    deliverEvent(otherSubscription, otherResponses, otherKey, 11);
                    assertThat(client.getKVClient().get(otherKey).get(5, TimeUnit.SECONDS)).isNotNull();
                    assertThat(slowResponses).isEmpty();
                    assertThat(other.isClosed()).isFalse();
                }

                release.complete(null);
                assertThat(nextResponse(slowResponses).getHeader().getRevision()).isEqualTo(11);
                assertThat(nextResponse(slowResponses).getHeader().getRevision()).isEqualTo(12);
                assertThat(slow.isClosed()).isFalse();
            } finally {
                release.complete(null);
            }
        } finally {
            await(vertx.close());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reconnectWaitsForAnInFlightCallbackToAdvanceTheRevision(boolean namespaced) throws Exception {
        var vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(2));
        BlockingQueue<Subscription> subscriptions = new LinkedBlockingQueue<>();
        BlockingQueue<WatchResponse> responses = new LinkedBlockingQueue<>();
        var key = TestUtil.bytesOf("key");
        var namespace = namespaced ? TestUtil.bytesOf("namespace/") : ByteSequence.EMPTY;
        var wireKey = namespace.concat(key);
        var entered = new CompletableFuture<Void>();
        var release = new CompletableFuture<Void>();
        var streamError = new CompletableFuture<Throwable>();
        var option = WatchOption.builder().withCreateNotify(true).build();

        try (Client client = startServer(vertx, subscriptions).namespace(namespace).build();
            var watcher = client.getWatchClient().watch(key, option, response -> {
                if (!response.isCreatedNotify() && response.getHeader().getRevision() == 11) {
                    entered.complete(null);
                    release.join();
                }
                responses.add(response);
            }, streamError::complete)) {
            try {
                var initial = nextSubscription(subscriptions, 0);
                acknowledge(initial, responses, 10);
                assertThat(initial.request().getKey()).isEqualTo(ByteString.copyFrom(wireKey.getBytes()));
                writeEvent(initial, wireKey, 11, false);
                entered.get(5, TimeUnit.SECONDS);
                await(initial.connection().close());
                assertThat(subscriptions.poll(200, TimeUnit.MILLISECONDS)).isNull();
                assertThat(streamError).isNotDone();

                release.complete(null);
                var delivered = nextResponse(responses);
                assertThat(delivered.getHeader().getRevision()).isEqualTo(11);
                assertThat(delivered.getEvents()).singleElement().satisfies(event -> {
                    assertThat(event.keyValue().getKey()).isEqualTo(key);
                });
                assertThat(streamError.get(5, TimeUnit.SECONDS)).isInstanceOf(EtcdException.class);
                var replacement = nextSubscription(subscriptions, 12);
                acknowledge(replacement, responses, 15);
                deliverEvent(replacement, responses, wireKey, 12);
                assertThat(watcher.isClosed()).isFalse();
            } finally {
                release.complete(null);
            }
        } finally {
            await(vertx.close());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void listenerCanCloseItsOwnWatcherBeforeCompletionIsDelivered(boolean createOnWorker) throws Exception {
        var vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(1));
        BlockingQueue<Subscription> subscriptions = new LinkedBlockingQueue<>();
        BlockingQueue<WatchResponse> responses = new LinkedBlockingQueue<>();
        BlockingQueue<String> order = new LinkedBlockingQueue<>();
        var current = new AtomicReference<Watch.Watcher>();
        var key = TestUtil.bytesOf("key");
        var option = WatchOption.builder().withCreateNotify(true).build();
        var listener = Watch.listener(response -> {
            if (response.isCreatedNotify()) {
                responses.add(response);
            } else {
                current.get().close();
                order.add("close returned");
            }
        }, () -> order.add("completed"));

        try (Client client = startServer(vertx, subscriptions).build()) {
            Supplier<Watch.Watcher> create = () -> client.getWatchClient().watch(key, option, listener);
            try (var watcher = createOnWorker ? startFromWorker(vertx, create) : create.get()) {
                current.set(watcher);
                var subscription = nextSubscription(subscriptions, 0);
                acknowledge(subscription, responses, 10);
                writeEvent(subscription, key, 11, false);

                assertThat(order.poll(5, TimeUnit.SECONDS)).isEqualTo("close returned");
                assertThat(order.poll(5, TimeUnit.SECONDS)).isEqualTo("completed");
                assertThat(watcher.isClosed()).isTrue();
            }
        } finally {
            await(vertx.close());
        }
    }

    private static Watch.Watcher startFromWorker(Vertx vertx, Supplier<Watch.Watcher> create) throws Exception {
        var watcher = new CompletableFuture<Watch.Watcher>();
        await(vertx.deployVerticle(new VerticleBase() {
            @Override
            public Future<?> start() {
                watcher.complete(create.get());
                return Future.succeededFuture();
            }
        }, new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)));
        return watcher.get(5, TimeUnit.SECONDS);
    }

    private static ClientBuilder startServer(Vertx vertx, BlockingQueue<Subscription> subscriptions) throws Exception {
        var clientMethod = io.etcd.jetcd.api.WatchGrpcClient.Watch;
        ServiceMethod<io.etcd.jetcd.api.WatchRequest, io.etcd.jetcd.api.WatchResponse> method = ServiceMethod.server(
            clientMethod.serviceName(), clientMethod.methodName(), clientMethod.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.WatchRequest.getDefaultInstance()));

        var grpcServer = GrpcServer.server(vertx).callHandler(method, request -> request.handler(message -> {
            if (message.hasCreateRequest()) {
                subscriptions.add(new Subscription(message.getCreateRequest(), request.response(), request.connection()));
            }
        }));
        var range = io.etcd.jetcd.api.KVGrpcClient.Range;
        ServiceMethod<io.etcd.jetcd.api.RangeRequest, io.etcd.jetcd.api.RangeResponse> rangeMethod = ServiceMethod.server(
            range.serviceName(), range.methodName(), range.cardinality(), GrpcMessageEncoder.encoder(),
            GrpcMessageDecoder.decoder(io.etcd.jetcd.api.RangeRequest.getDefaultInstance()));
        grpcServer.callHandler(rangeMethod,
            request -> request
                .handler(message -> request.response().end(io.etcd.jetcd.api.RangeResponse.getDefaultInstance())));
        var server = await(vertx.createHttpServer().requestHandler(grpcServer).listen(0, EtcdConstants.LOCALHOST));
        return Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + server.actualPort()).vertx(vertx);
    }

    private static Subscription nextSubscription(BlockingQueue<Subscription> subscriptions, long revision)
        throws InterruptedException {
        var subscription = subscriptions.poll(5, TimeUnit.SECONDS);
        assertThat(subscription).as("subscription from revision %s", revision).isNotNull();
        assertThat(subscription.request().getStartRevision()).as("requested revision").isEqualTo(revision);
        return subscription;
    }

    private static void acknowledge(Subscription subscription, BlockingQueue<WatchResponse> responses, long revision)
        throws Exception {
        await(subscription.stream().write(io.etcd.jetcd.api.WatchResponse.newBuilder()
            .setCreated(true).setWatchId(1)
            .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setRevision(revision)).build()));
        var acknowledgment = responses.poll(5, TimeUnit.SECONDS);
        assertThat(acknowledgment).as("creation acknowledgment at revision %s", revision).isNotNull();
        assertThat(acknowledgment.isCreatedNotify()).isTrue();
        assertThat(acknowledgment.getHeader().getRevision()).isEqualTo(revision);
    }

    private static void deliverEvent(Subscription subscription, BlockingQueue<WatchResponse> responses, ByteSequence key,
        long revision)
        throws Exception {
        writeEvent(subscription, key, revision, false);
        var response = nextResponse(responses);
        assertThat(response.getEvents()).singleElement()
            .satisfies(event -> assertThat(event.keyValue().getModRevision()).isEqualTo(revision));
    }

    private static WatchResponse nextResponse(BlockingQueue<WatchResponse> responses) throws InterruptedException {
        var response = responses.poll(5, TimeUnit.SECONDS);
        assertThat(response).as("watch response").isNotNull();
        return response;
    }

    private static void writeEvent(Subscription subscription, ByteSequence key, long revision, boolean fragment)
        throws Exception {
        await(subscription.stream().write(io.etcd.jetcd.api.WatchResponse.newBuilder()
            .setWatchId(1).setFragment(fragment)
            .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setRevision(revision))
            .addEvents(io.etcd.jetcd.api.Event.newBuilder()
                .setKv(io.etcd.jetcd.api.KeyValue.newBuilder().setKey(ByteString.copyFrom(key.getBytes()))
                    .setModRevision(revision)))
            .build()));
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private record Subscription(io.etcd.jetcd.api.WatchCreateRequest request,
        WriteStream<io.etcd.jetcd.api.WatchResponse> stream, HttpConnection connection) {
    }
}
