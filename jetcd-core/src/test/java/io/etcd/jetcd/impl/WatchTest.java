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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
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

@Tag("watch")
@Timeout(value = 30)
public class WatchTest {

    private static final long TIME_OUT_SECONDS = 30;

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    public static final ByteSequence namespace = bytesOf("test-namespace/");

    static Stream<Arguments> parameters() {
        return Stream.of(
            arguments(true), // use namespace
            arguments(false)); // no namespace
    }

    private static Client createClient(boolean useNamespace) {
        if (useNamespace) {
            return TestUtil.client(cluster).namespace(namespace).build();
        } else {
            return TestUtil.client(cluster).build();
        }
    }

    @Test
    public void testNamespacedAndNotNamespacedClient() throws Exception {
        final ByteSequence key = randomByteSequence();
        final ByteSequence nsKey = ByteSequence.from(namespace.concat(key).getBytes());
        final ByteSequence value = randomByteSequence();
        final AtomicReference<WatchResponse> ref = new AtomicReference<>();

        try (Client client = TestUtil.client(cluster).build();
            Client nsClient = TestUtil.client(cluster).namespace(namespace).build()) {

            // From client with namespace watch for key. Since client is namespaced it should watch for namespaced key.
            try (Watcher watcher = nsClient.getWatchClient().watchAsync(key, ref::set).get(5, TimeUnit.SECONDS)) { // NOPMD - UnusedLocalVariable
                // Using non-namespaced client put namespaced key.
                client.getKVClient().put(nsKey, value).get();
                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

                assertThat(ref.get()).isNotNull();
                assertThat(ref.get().getEvents().size()).isEqualTo(1);
                assertThat(ref.get().getEvents().get(0).eventType()).isEqualTo(EventType.PUT);
                assertThat(ref.get().getEvents().get(0).keyValue().getKey()).isEqualTo(key);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchOnPut(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final ByteSequence value = randomByteSequence();
            final AtomicReference<WatchResponse> ref = new AtomicReference<>();

            try (Watcher watcher = client.getWatchClient().watchAsync(key, ref::set).get(5, TimeUnit.SECONDS)) { // NOPMD - UnusedLocalVariable

                client.getKVClient().put(key, value).get();

                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

                assertThat(ref.get()).isNotNull();
                assertThat(ref.get().getEvents().size()).isEqualTo(1);
                assertThat(ref.get().getEvents().get(0).eventType()).isEqualTo(EventType.PUT);
                assertThat(ref.get().getEvents().get(0).keyValue().getKey()).isEqualTo(key);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testMultipleWatch(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final ByteSequence value = randomByteSequence();
            final List<WatchResponse> res = Collections.synchronizedList(new ArrayList<>(2));

            // Use watchAsync to ensure watchers are ready before putting
            try (Watcher w1 = client.getWatchClient().watchAsync(key, res::add).get(5, TimeUnit.SECONDS); // NOPMD - UnusedLocalVariable
                Watcher w2 = client.getWatchClient().watchAsync(key, res::add).get(5, TimeUnit.SECONDS)) { // NOPMD - UnusedLocalVariable

                client.getKVClient().put(key, value).get();

                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(res).hasSize(2));
                // Both watchers should receive responses with the same events
                assertThat(res.get(0).getEvents()).usingRecursiveComparison().isEqualTo(res.get(1).getEvents());
                // Verify cluster_id and revision are the same (member_id can differ)
                assertThat(res.get(0).getHeader().getClusterId()).isEqualTo(res.get(1).getHeader().getClusterId());
                assertThat(res.get(0).getHeader().getRevision()).isEqualTo(res.get(1).getHeader().getRevision());
                assertThat(res.get(0).getEvents().size()).isEqualTo(1);
                assertThat(res.get(0).getEvents().get(0).eventType()).isEqualTo(EventType.PUT);
                assertThat(res.get(0).getEvents().get(0).keyValue().getKey()).isEqualTo(key);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchOnDelete(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final ByteSequence value = randomByteSequence();
            final AtomicReference<WatchResponse> ref = new AtomicReference<>();

            client.getKVClient().put(key, value).get();

            // Use watchAsync to ensure watcher is ready before deleting
            try (Watcher watcher = client.getWatchClient().watchAsync(key, ref::set).get(5, TimeUnit.SECONDS)) { // NOPMD - UnusedLocalVariable
                client.getKVClient().delete(key).get();

                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

                assertThat(ref.get().getEvents().size()).isEqualTo(1);

                WatchEvent event = ref.get().getEvents().get(0);
                assertThat(event.eventType()).isEqualTo(EventType.DELETE);
                assertThat(Arrays.equals(event.keyValue().getKey().getBytes(), key.getBytes())).isTrue();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchCompacted(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();

            final AtomicReference<Throwable> ref = new AtomicReference<>();
            // Try to listen from previous revision on
            final WatchOption options = WatchOption.builder().withRevision(getCompactedRevision(client, key)).build();
            final Watch wc = client.getWatchClient();

            try (Watcher watcher = wc.watch(key, options, Watch.listener(TestUtil::noOpWatchResponseConsumer, ref::set))) { // NOPMD - UnusedLocalVariable
                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());
                assertThat(ref.get().getClass()).isEqualTo(CompactedException.class);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchClose(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final ByteSequence value = randomByteSequence();
            final List<WatchResponse> events = Collections.synchronizedList(new ArrayList<>());

            try (Watcher watcher = client.getWatchClient().watchAsync(key, events::add).get(5, TimeUnit.SECONDS)) { // NOPMD - UnusedLocalVariable
                client.getKVClient().put(key, value).get();
                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(events).isNotEmpty());
            }

            client.getKVClient().put(key, randomByteSequence()).get();

            await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(events).hasSize(1));
            assertThat(events.get(0).getEvents()).hasSize(1);
            assertThat(events.get(0).getEvents().get(0).eventType()).isEqualTo(EventType.PUT);
            assertThat(events.get(0).getEvents().get(0).keyValue().getKey()).isEqualTo(key);
            assertThat(events.get(0).getEvents().get(0).keyValue().getValue()).isEqualTo(value);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testProgressRequest(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final ByteSequence value = randomByteSequence();
            final Watch watchClient = client.getWatchClient();
            final AtomicReference<WatchResponse> emptyWatcherEventRef = new AtomicReference<>();
            final AtomicReference<WatchResponse> activeWatcherEventRef = new AtomicReference<>();

            try (Watcher activeWatcher = watchClient.watch(key, activeWatcherEventRef::set); // NOPMD - UnusedLocalVariable
                Watcher emptyWatcher = watchClient.watch(key.concat(randomByteSequence()), emptyWatcherEventRef::set)) { // NOPMD - UnusedLocalVariable
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
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchFromFutureRevisionReceivesOnlyFutureEvents(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final ByteSequence value = randomByteSequence();
            BlockingQueue<WatchResponse> responses = new LinkedBlockingQueue<>();
            var kv = client.getKVClient();
            long lastSeenRevision = kv.put(key, randomByteSequence()).get(5, TimeUnit.SECONDS).getHeader().getRevision();
            var watchOption = WatchOption.builder().withRevision(lastSeenRevision + 1).withCreateNotify(true).build();

            try (Watcher watcher = client.getWatchClient().watch(key, watchOption, responses::add)) {
                var created = responses.poll(5, TimeUnit.SECONDS);
                assertThat(created).as("watch creation acknowledgment").isNotNull();
                assertThat(created.isCreatedNotify()).isTrue();

                long revision = kv.put(key, value).get(5, TimeUnit.SECONDS).getHeader().getRevision();
                var updated = responses.poll(5, TimeUnit.SECONDS);
                assertThat(updated).as("first event from the requested revision").isNotNull();
                assertThat(updated.getEvents()).singleElement().satisfies(event -> {
                    assertThat(event.keyValue().getKey()).isEqualTo(key);
                    assertThat(event.keyValue().getValue()).isEqualTo(value);
                    assertThat(event.keyValue().getModRevision()).isEqualTo(revision);
                });
                assertThat(watcher.isClosed()).isFalse();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchAndGet(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final ByteSequence value = randomByteSequence();
            final AtomicReference<KeyValue> ref = new AtomicReference<>();

            final Consumer<WatchResponse> consumer = response -> {
                for (WatchEvent event : response.getEvents()) {
                    if (event.eventType() == EventType.PUT) {
                        ByteSequence key1 = event.keyValue().getKey();

                        Future<?> unused = client.getKVClient().get(key1).whenComplete((r, t) -> {
                            if (!r.getKvs().isEmpty()) {
                                ref.set(r.getKvs().get(0));
                            }
                        });
                    }
                }
            };

            try (Watcher watcher = client.getWatchClient().watchAsync(key, consumer).get(5, TimeUnit.SECONDS)) { // NOPMD - UnusedLocalVariable
                client.getKVClient().put(key, value).get();

                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

                assertThat(ref.get()).isNotNull();
                assertThat(ref.get().getKey()).isEqualTo(key);
                assertThat(ref.get().getValue()).isEqualTo(value);
            }
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
    public void testCancelledWatchGetsClosed(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final Watch wc = client.getWatchClient();

            long revision = getCompactedRevision(client, key);
            final WatchOption options = WatchOption.builder().withRevision(revision).build();

            final AtomicReference<Throwable> ref = new AtomicReference<>();
            final AtomicReference<Boolean> completed = new AtomicReference<>();

            Watch.Listener listener = Watch.listener(TestUtil::noOpWatchResponseConsumer, ref::set, () -> {
                completed.set(Boolean.TRUE);
            });

            try (Watcher watcher = wc.watch(key, options, listener)) { // NOPMD - UnusedLocalVariable
                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());
                assertThat(ref.get().getClass()).isEqualTo(CompactedException.class);
                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(completed.get()).isTrue());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchWithCreatedNotify(boolean useNamespace) throws Exception {
        try (Client client = createClient(useNamespace)) {
            final ByteSequence key = randomByteSequence();
            final WatchOption options = WatchOption.builder().withCreateNotify(true).build();
            final AtomicReference<WatchResponse> ref = new AtomicReference<>();

            try (Watcher watcher = client.getWatchClient().watch(key, options, ref::set)) { // NOPMD - UnusedLocalVariable
                await().atMost(TIME_OUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());
                assertThat(ref.get().getEvents().size()).isEqualTo(0);
                assertThat(ref.get().isCreatedNotify()).isEqualTo(true);
            }
        }
    }
}
