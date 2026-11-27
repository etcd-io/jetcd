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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.etcd.jetcd.test.EtcdConstants;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchEvent.EventType;
import io.etcd.jetcd.watch.WatchResponse;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("watch")
@Timeout(30)
public class WatchResumeTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .withMountDirectory(false)
        .build();

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = 2 * 1024 * 1024)
    void deliversALargeFragmentedRevisionAsOneResponse(Integer maxInboundMessageSize) throws Exception {
        var prefix = TestUtil.randomByteSequence().concat(TestUtil.bytesOf("/"));
        var first = prefix.concat(TestUtil.bytesOf("first"));
        var second = prefix.concat(TestUtil.bytesOf("second"));
        // Two values fit in one transaction. The watch includes both old values, so etcd fragments the response.
        var previous = ByteSequence.from(new byte[768 * 1024]);
        var updated = ByteSequence.from(new byte[384 * 1024]);

        try (Client client = TestUtil.client(cluster).maxInboundMessageSize(maxInboundMessageSize).build()) {
            var kv = client.getKVClient();
            kv.put(first, previous).get(5, TimeUnit.SECONDS);
            kv.put(second, previous).get(5, TimeUnit.SECONDS);
            var option = WatchOption.builder().isPrefix(true).withCreateNotify(true).withPrevKV(true).withFragment(true)
                .build();
            var created = new CompletableFuture<Void>();
            var received = new CompletableFuture<WatchResponse>();

            try (Watcher watcher = client.getWatchClient().watch(prefix, option, response -> {
                if (response.isCreatedNotify()) {
                    created.complete(null);
                } else {
                    received.complete(response);
                }
            }, error -> {
                created.completeExceptionally(error);
                received.completeExceptionally(error);
            })) {
                created.get(5, TimeUnit.SECONDS);
                long revision = kv.txn()
                    .Then(Op.put(first, updated, PutOption.DEFAULT), Op.put(second, updated, PutOption.DEFAULT))
                    .commit().get(5, TimeUnit.SECONDS).getHeader().getRevision();
                var events = received.get(5, TimeUnit.SECONDS).getEvents();
                assertThat(events).extracting(event -> event.keyValue().getKey()).containsExactlyInAnyOrder(first, second);
                assertThat(events).allSatisfy(event -> {
                    assertThat(event.keyValue().getModRevision()).isEqualTo(revision);
                    assertThat(event.keyValue().getValue()).isEqualTo(updated);
                    assertThat(event.prevKV().getValue()).isEqualTo(previous);
                });
                assertThat(watcher.isClosed()).isFalse();
            }
        }
    }

    @Test
    void replaysWritesMadeWhileDisconnected() throws Exception {
        var endpoint = cluster.clientEndpoints().getFirst();
        var vertx = Vertx.vertx();
        var watchSocket = new AtomicReference<NetSocket>();
        var forwardConnections = new AtomicReference<CompletableFuture<Void>>(CompletableFuture.completedFuture(null));
        var resumeForwarding = new CompletableFuture<Void>();

        try {
            var upstream = vertx.createNetClient();
            var proxy = vertx.createNetServer()
                .connectHandler(socket -> {
                    socket.pause();
                    Future.fromCompletionStage(forwardConnections.get(), Vertx.currentContext())
                        .compose(ignored -> upstream.connect(endpoint.getPort(), endpoint.getHost()))
                        .onSuccess(etcdSocket -> {
                            watchSocket.set(socket);
                            forward(socket, etcdSocket);
                        })
                        .onFailure(error -> socket.close());
                })
                .listen(0, EtcdConstants.LOCALHOST)
                .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            var key = TestUtil.randomByteSequence();
            var before = TestUtil.bytesOf("before disconnect");
            var missed = TestUtil.bytesOf("while disconnected");
            var after = TestUtil.bytesOf("after reconnect");
            var events = new LinkedBlockingQueue<WatchEvent>();
            var created = new CompletableFuture<Void>();
            var options = WatchOption.builder().withCreateNotify(true).build();

            try (Client writer = TestUtil.client(cluster).build();
                Client reader = Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + proxy.actualPort()).build();
                Watcher watcher = reader.getWatchClient().watch(key, options, response -> {
                    if (response.isCreatedNotify()) {
                        created.complete(null);
                    }
                    events.addAll(response.getEvents());
                })) {
                var kv = writer.getKVClient();

                // Wait for etcd to create the initial watch before writing the first value.
                created.get(5, TimeUnit.SECONDS);
                kv.put(key, before).get(5, TimeUnit.SECONDS);
                assertPut(events, key, before);

                // Hold replacement connections until the direct client has written the missed event.
                forwardConnections.set(resumeForwarding);
                watchSocket.get().close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
                kv.put(key, missed).get(5, TimeUnit.SECONDS);
                resumeForwarding.complete(null);
                assertPut(events, key, missed);

                kv.put(key, after).get(5, TimeUnit.SECONDS);
                assertPut(events, key, after);
                assertThat(events).isEmpty();
                assertThat(watcher.isClosed()).isFalse();
            }
        } finally {
            resumeForwarding.cancel(false);
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    private static void forward(NetSocket from, NetSocket to) {
        from.closeHandler(ignored -> to.close());
        to.closeHandler(ignored -> from.close());
        from.pipeTo(to);
        to.pipeTo(from);
    }

    private static void assertPut(BlockingQueue<WatchEvent> events, ByteSequence key, ByteSequence value)
        throws InterruptedException {
        var event = events.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.eventType()).isEqualTo(EventType.PUT);
        assertThat(event.keyValue().getKey()).isEqualTo(key);
        assertThat(event.keyValue().getValue()).isEqualTo(value);
    }
}
