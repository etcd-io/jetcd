/**
 * Copyright 2017 The jetcd authors
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

import static org.assertj.core.api.Assertions.assertThat;

import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.common.exception.CompactedException;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.launcher.junit.EtcdClusterResource;
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
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * watch test case.
 */
public class WatchTest {
  @ClassRule
  public static final EtcdClusterResource clusterResource = new EtcdClusterResource("watch", 3);

  private static Client client;
  private static Watch watchClient;
  private static KV kvClient;

  @Rule
  public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp() {
    client = Client.builder().endpoints(clusterResource.cluster().getClientEndpoints()).build();
    watchClient = client.getWatchClient();
    kvClient = client.getKVClient();
  }

  @AfterClass
  public static void tearDown() {
    client.close();
  }

  @Test
  public void testWatchOnPut() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    ByteSequence key = TestUtil.randomByteSequence();
    ByteSequence value = TestUtil.randomByteSequence();
    AtomicReference<WatchResponse> ref = new AtomicReference<>();

    Watcher watcher = null;

    try {
      watcher = watchClient.watch(key, response -> {
        ref.set(response);
        latch.countDown();
      });

      kvClient.put(key, value).get();
      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getEvents().size()).isEqualTo(1);
      assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
      assertThat(ref.get().getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    } finally {
      IOUtils.closeQuietly(watcher);
    }
  }

  @Test
  public void testMultipleWatch() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    ByteSequence key = TestUtil.randomByteSequence();
    ByteSequence value = TestUtil.randomByteSequence();
    List<WatchResponse> res = Collections.synchronizedList(new ArrayList<>(2));

    Watcher w1 = null;
    Watcher w2 = null;

    try {
      w1 = watchClient.watch(key, response -> {
        res.add(response);
        latch.countDown();
      });

      w2 = watchClient.watch(key, response -> {
        res.add(response);
        latch.countDown();
      });

      kvClient.put(key, value).get();
      latch.await(4, TimeUnit.SECONDS);

      assertThat(res).hasSize(2);
      assertThat(res.get(0)).isEqualToComparingFieldByFieldRecursively(res.get(1));
      assertThat(res.get(0).getEvents().size()).isEqualTo(1);
      assertThat(res.get(0).getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
      assertThat(res.get(0).getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    } finally {
      IOUtils.closeQuietly(w1);
      IOUtils.closeQuietly(w2);
    }
  }

  @Test
  public void testWatchOnDelete() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    ByteSequence key = TestUtil.randomByteSequence();
    ByteSequence value = TestUtil.randomByteSequence();
    AtomicReference<WatchResponse> ref = new AtomicReference<>();

    Watcher watcher = null;

    try {
      Watch.Listener listener = Watch.listener( response -> {
        ref.set(response);
        latch.countDown();
      });

      kvClient.put(key, value).get();

      watcher = watchClient.watch(key, listener);
      kvClient.delete(key).get();

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getEvents().size()).isEqualTo(1);

      WatchEvent event = ref.get().getEvents().get(0);
      assertThat(event.getEventType()).isEqualTo(EventType.DELETE);
      assertThat(Arrays.equals(event.getKeyValue().getKey().getBytes(), key.getBytes())).isTrue();
    } finally {
      IOUtils.closeQuietly(watcher);
    }
  }

  @Test
  public void testWatchCompacted() throws Exception {
    ByteSequence key = TestUtil.randomByteSequence();
    ByteSequence value = TestUtil.randomByteSequence();
    // Insert key twice to ensure we have at least two revisions
    kvClient.put(key, value).get();
    PutResponse putResponse = kvClient.put(key, value).get();
    // Compact until latest revision
    kvClient.compact(putResponse.getHeader().getRevision()).get();

    Watcher watcher = null;
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ref = new AtomicReference<>();
    try {
      // Try to listen from previous revision on
      WatchOption watchOption = WatchOption.newBuilder().withRevision(putResponse.getHeader().getRevision() - 1)
          .build();
      watcher = watchClient.watch(key, watchOption, Watch.listener(response -> {}, error -> {
        ref.set(error);
        latch.countDown();
      }));

      latch.await(4, TimeUnit.SECONDS);
      // Expect CompactedException
      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getClass()).isEqualTo(CompactedException.class);
    } finally {
      IOUtils.closeQuietly(watcher);
    }
  }
}
