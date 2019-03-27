/*
 * Copyright 2016-2019 The jetcd authors
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

package io.etcd.jetcd;

import static io.etcd.jetcd.TestUtil.bytesOf;
import static io.etcd.jetcd.TestUtil.randomByteSequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.common.exception.CompactedException;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.launcher.junit5.EtcdClusterExtension;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchEvent.EventType;
import io.etcd.jetcd.watch.WatchResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * watch test case.
 */
// TODO(#548): Add global timeout for tests once JUnit5 supports it
public class WatchTest {

  @RegisterExtension
  public static final EtcdClusterExtension cluster = new EtcdClusterExtension("watch", 3);
  private static final ByteSequence namespace = bytesOf("test-namespace/");

  static Stream<Arguments> parameters() {
    return Stream.of(
        arguments(Client.builder().endpoints(cluster.getClientEndpoints()).namespace(namespace).build()),
        arguments(Client.builder().endpoints(cluster.getClientEndpoints()).build())
    );
  }

  @Test
  public void testNamespacedAndNotNamespacedClient() throws Exception {
    final ByteSequence key = randomByteSequence();
    final ByteSequence nsKey = ByteSequence.from(namespace.getByteString().concat(key.getByteString()));
    final Client client = Client.builder().endpoints(cluster.getClientEndpoints()).build();
    final Client nsClient = Client.builder().endpoints(cluster.getClientEndpoints()).namespace(namespace).build();

    final CountDownLatch latch = new CountDownLatch(1);
    final ByteSequence value = randomByteSequence();
    final AtomicReference<WatchResponse> ref = new AtomicReference<>();

    // From client with namespace watch for key. Since client is namespaced it should watch for namespaced key.
    try (Watcher watcher = nsClient.getWatchClient().watch(key, response -> {
      ref.set(response);
      latch.countDown();
    })) {

      // Using non-namespaced client put namespaced key.
      client.getKVClient().put(nsKey, value).get();
      latch.await(4, TimeUnit.SECONDS);

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
    final CountDownLatch latch = new CountDownLatch(1);
    final ByteSequence value = randomByteSequence();
    final AtomicReference<WatchResponse> ref = new AtomicReference<>();

    try (Watcher watcher = client.getWatchClient().watch(key, response -> {
      ref.set(response);
      latch.countDown();
    })) {

      client.getKVClient().put(key, value).get();
      latch.await(4, TimeUnit.SECONDS);

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

    try (Watcher w1 = client.getWatchClient().watch(key, response -> {
      res.add(response);
      latch.countDown();
    })) {

      try (Watcher w2 = client.getWatchClient().watch(key, response -> {
        res.add(response);
        latch.countDown();
      })) {

        client.getKVClient().put(key, value).get();
        latch.await(4, TimeUnit.SECONDS);

        assertThat(res).hasSize(2);
        assertThat(res.get(0)).isEqualToComparingFieldByFieldRecursively(res.get(1));
        assertThat(res.get(0).getEvents().size()).isEqualTo(1);
        assertThat(res.get(0).getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
        assertThat(res.get(0).getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void testWatchOnDelete(final Client client) throws Exception {
    final ByteSequence key = randomByteSequence();
    final CountDownLatch latch = new CountDownLatch(1);
    final ByteSequence value = randomByteSequence();
    final AtomicReference<WatchResponse> ref = new AtomicReference<>();

    client.getKVClient().put(key, value).get();

    try (Watcher watcher = client.getWatchClient().watch(key, response -> {
      ref.set(response);
      latch.countDown();
    })) {
      client.getKVClient().delete(key).get();

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
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
    final ByteSequence value = randomByteSequence();

    // Insert key twice to ensure we have at least two revisions
    client.getKVClient().put(key, value).get();
    final PutResponse putResponse = client.getKVClient().put(key, value).get();
    // Compact until latest revision
    client.getKVClient().compact(putResponse.getHeader().getRevision()).get();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> ref = new AtomicReference<>();
    // Try to listen from previous revision on
    final WatchOption watchOption = WatchOption.newBuilder().withRevision(putResponse.getHeader().getRevision() - 1)
        .build();
    try (Watcher watcher = client.getWatchClient().watch(key, watchOption, Watch.listener(response -> {
    }, error -> {
      ref.set(error);
      latch.countDown();
    }))) {

      latch.await(4, TimeUnit.SECONDS);
      // Expect CompactedException
      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getClass()).isEqualTo(CompactedException.class);
    }
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void testWatchClose(final Client client) throws Exception {
    final ByteSequence key = randomByteSequence();
    final CountDownLatch latch = new CountDownLatch(2);
    final ByteSequence value = randomByteSequence();
    final AtomicReference<WatchResponse> ref = new AtomicReference<>();

    try (Watcher watcher = client.getWatchClient().watch(key, response -> {
      ref.set(response);
      latch.countDown();
    })) {
      client.getKVClient().put(key, value).get();
      latch.await(4, TimeUnit.SECONDS);
    }

    client.getKVClient().put(key, value).get();

    latch.await(4, TimeUnit.SECONDS);

    assertThat(ref.get()).isNotNull();
    assertThat(ref.get().getEvents()).hasSize(1);
    assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
    assertThat(ref.get().getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    assertThat(latch.getCount()).isEqualTo(1);
  }
}
