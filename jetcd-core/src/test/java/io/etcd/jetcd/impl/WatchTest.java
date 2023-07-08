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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.common.exception.CompactedException;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchEvent.EventType;
import io.etcd.jetcd.watch.WatchResponse;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static io.etcd.jetcd.impl.TestUtil.randomByteSequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Timeout(value = 30)
public class WatchTest {

    private static final long TIME_OUT_SECONDS = 30;

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(3)
        .build();

    public static final ByteSequence namespace = bytesOf("test-namespace/");

    static Stream<Arguments> parameters() {
        return Stream.of(
            arguments(TestUtil.client(cluster).namespace(namespace).build()),
            arguments(TestUtil.client(cluster).build()));
    }

    @Test
    public void testNamespacedAndNotNamespacedClient() throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence nsKey = ByteSequence.from(namespace.concat(key).getBytes());
        final Client client = TestUtil.client(cluster).build();
        final Client nsClient = TestUtil.client(cluster).namespace(namespace).build();

        final ByteSequence value = randomByteSequence();
        final AtomicReference<WatchResponse> ref = new AtomicReference<>();

        // From client with namespace watch for key. Since client is namespaced it should watch for namespaced key.
        try (Watcher watcher = nsClient.getWatchClient().watch(key, ref::set)) {
            // Using non-namespaced client put namespaced key.
            client.getKVClient().put(nsKey, value).get();
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

            assertThat(ref.get()).isNotNull();
            assertThat(ref.get().getEvents().size()).isEqualTo(1);
            assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
            assertThat(ref.get().getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchOnPut(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence value = randomByteSequence();
        final AtomicReference<WatchResponse> ref = new AtomicReference<>();

        try (Watcher watcher = client.getWatchClient().watch(key, ref::set)) {

            client.getKVClient().put(key, value).get();

            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

            assertThat(ref.get()).isNotNull();
            assertThat(ref.get().getEvents().size()).isEqualTo(1);
            assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
            assertThat(ref.get().getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testMultipleWatch(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final CountDownLatch latch = new CountDownLatch(2);
        final ByteSequence value = randomByteSequence();
        final List<WatchResponse> res = Collections.synchronizedList(new ArrayList<>(2));

        try (Watcher w1 = client.getWatchClient().watch(key, res::add);
            Watcher w2 = client.getWatchClient().watch(key, res::add)) {

            client.getKVClient().put(key, value).get();
            latch.await(4, TimeUnit.SECONDS);

            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(res).hasSize(2));
            assertThat(res.get(0)).usingRecursiveComparison().isEqualTo(res.get(1));
            assertThat(res.get(0).getEvents().size()).isEqualTo(1);
            assertThat(res.get(0).getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
            assertThat(res.get(0).getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchOnDelete(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence value = randomByteSequence();
        final AtomicReference<WatchResponse> ref = new AtomicReference<>();

        client.getKVClient().put(key, value).get();

        try (Watcher watcher = client.getWatchClient().watch(key, ref::set)) {
            client.getKVClient().delete(key).get();

            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

            assertThat(ref.get().getEvents().size()).isEqualTo(1);

            WatchEvent event = ref.get().getEvents().get(0);
            assertThat(event.getEventType()).isEqualTo(EventType.DELETE);
            assertThat(Arrays.equals(event.getKeyValue().getKey().getBytes(), key.getBytes())).isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchCompacted(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();

        final AtomicReference<Throwable> ref = new AtomicReference<>();
        // Try to listen from previous revision on
        final WatchOption options = WatchOption.builder().withRevision(getCompactedRevision(client, key)).build();
        final Watch wc = client.getWatchClient();

        try (Watcher watcher = wc.watch(key, options, Watch.listener(TestUtil::noOpWatchResponseConsumer, ref::set))) {
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());
            assertThat(ref.get().getClass()).isEqualTo(CompactedException.class);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchClose(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence value = randomByteSequence();
        final List<WatchResponse> events = Collections.synchronizedList(new ArrayList<>());

        try (Watcher watcher = client.getWatchClient().watch(key, events::add)) {
            client.getKVClient().put(key, value).get();
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(events).isNotEmpty());
        }

        client.getKVClient().put(key, randomByteSequence()).get();

        await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(events).hasSize(1));
        assertThat(events.get(0).getEvents()).hasSize(1);
        assertThat(events.get(0).getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
        assertThat(events.get(0).getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
        assertThat(events.get(0).getEvents().get(0).getKeyValue().getValue()).isEqualTo(value);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testProgressRequest(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence value = randomByteSequence();
        final Watch watchClient = client.getWatchClient();
        final AtomicReference<WatchResponse> emptyWatcherEventRef = new AtomicReference<>();
        final AtomicReference<WatchResponse> activeWatcherEventRef = new AtomicReference<>();

        try (Watcher activeWatcher = watchClient.watch(key, activeWatcherEventRef::set);
            Watcher emptyWatcher = watchClient.watch(key.concat(randomByteSequence()), emptyWatcherEventRef::set)) {
            // Check that a requestProgress returns identical revisions initially
            watchClient.requestProgress();
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(activeWatcherEventRef.get()).isNotNull();
                assertThat(emptyWatcherEventRef.get()).isNotNull();
            });
            WatchResponse activeEvent = activeWatcherEventRef.get();
            WatchResponse emptyEvent = emptyWatcherEventRef.get();
            assertThat(activeEvent).satisfies(WatchResponse::isProgressNotify);
            assertThat(emptyEvent).satisfies(WatchResponse::isProgressNotify);
            assertThat(activeEvent.getHeader().getRevision()).isEqualTo(emptyEvent.getHeader().getRevision());

            // Put a value being watched by only the active watcher
            activeWatcherEventRef.set(null);
            emptyWatcherEventRef.set(null);
            client.getKVClient().put(key, value).get();
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(activeWatcherEventRef.get()).isNotNull();
            });
            activeEvent = activeWatcherEventRef.get();
            emptyEvent = emptyWatcherEventRef.get();
            assertThat(emptyEvent).isNull();
            assertThat(activeEvent).isNotNull();
            long latestRevision = activeEvent.getHeader().getRevision();

            // verify the next progress notify brings both watchers to the latest revision
            activeWatcherEventRef.set(null);
            emptyWatcherEventRef.set(null);
            watchClient.requestProgress();
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(activeWatcherEventRef.get()).isNotNull();
                assertThat(emptyWatcherEventRef.get()).isNotNull();
            });
            activeEvent = activeWatcherEventRef.get();
            emptyEvent = emptyWatcherEventRef.get();
            assertThat(activeEvent).satisfies(WatchResponse::isProgressNotify);
            assertThat(emptyEvent).satisfies(WatchResponse::isProgressNotify);
            assertThat(activeEvent.getHeader().getRevision()).isEqualTo(emptyEvent.getHeader().getRevision())
                .isEqualTo(latestRevision);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchFutureRevisionIsNotOverwrittenOnCreation(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence value = randomByteSequence();
        final List<WatchResponse> events = Collections.synchronizedList(new ArrayList<>());

        PutResponse putResponse = client.getKVClient().put(key, value).get();

        long lastSeenRevision = putResponse.getHeader().getRevision();
        WatchOption watchOption = WatchOption.builder().withRevision(lastSeenRevision + 1).build();

        try (Watcher watcher = client.getWatchClient().watch(key, watchOption, events::add)) {

            cluster.restart(); // resumes (recreates) the watch

            Thread.sleep(2000); // await().duration() would be better but it's broken
            assertThat(events.isEmpty()).as("verify that received events list is empty").isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchAndGet(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence value = randomByteSequence();
        final AtomicReference<KeyValue> ref = new AtomicReference<>();

        final Consumer<WatchResponse> consumer = response -> {
            for (WatchEvent event : response.getEvents()) {
                if (event.getEventType() == EventType.PUT) {
                    ByteSequence key1 = event.getKeyValue().getKey();

                    Future<?> unused = client.getKVClient().get(key1).whenComplete((r, t) -> {
                        if (!r.getKvs().isEmpty()) {
                            ref.set(r.getKvs().get(0));
                        }
                    });
                }
            }
        };

        try (Watcher watcher = client.getWatchClient().watch(key, consumer)) {
            client.getKVClient().put(key, value).get();

            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

            assertThat(ref.get()).isNotNull();
            assertThat(ref.get().getKey()).isEqualTo(key);
            assertThat(ref.get().getValue()).isEqualTo(value);
        }
    }

    private static long getCompactedRevision(final Client client, final ByteSequence key) throws Exception {
        final ByteSequence value = randomByteSequence();

        // Insert key twice to ensure we have at least two revisions
        client.getKVClient().put(key, value).get();
        final PutResponse putResponse = client.getKVClient().put(key, value).get();
        // Compact until latest revision
        client.getKVClient().compact(putResponse.getHeader().getRevision()).get();

        return putResponse.getHeader().getRevision() - 1;
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCancelledWatchGetsClosed(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final Watch wc = client.getWatchClient();

        long revision = getCompactedRevision(client, key);
        final WatchOption options = WatchOption.builder().withRevision(revision).build();

        final AtomicReference<Throwable> ref = new AtomicReference<>();
        final AtomicReference<Boolean> completed = new AtomicReference<>();

        Watch.Listener listener = Watch.listener(TestUtil::noOpWatchResponseConsumer, ref::set, () -> {
            completed.set(Boolean.TRUE);
        });

        try (Watcher watcher = wc.watch(key, options, listener)) {
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());
            assertThat(ref.get().getClass()).isEqualTo(CompactedException.class);
            assertThat(completed.get()).isNotNull();
            assertThat(completed.get()).isEqualTo(Boolean.TRUE);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchWithCreatedNotify(final Client client) throws Exception {

        final ByteSequence key = randomByteSequence();
        final WatchOption options = WatchOption.builder().withCreateNotify(true).build();
        final AtomicReference<WatchResponse> ref = new AtomicReference<>();

        try (Watcher watcher = client.getWatchClient().watch(key, options, ref::set)) {
            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());
            assertThat(ref.get().getEvents().size()).isEqualTo(0);
            assertThat(ref.get().isCreatedNotify()).isEqualTo(Boolean.TRUE);
        }
    }
}
