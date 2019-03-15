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

import com.google.common.base.Charsets;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * watch test case.
 */
@RunWith(Parameterized.class)
public class WatchTest {
  @ClassRule
  public static final EtcdClusterResource clusterResource = new EtcdClusterResource("watch", 3);
  private static final ByteSequence namespace = ByteSequence.from("test-namespace/", Charsets.UTF_8);

  private static Client client;
  private static Client clientWithNamespace;
  private static KV kvClient;

  private boolean useNamespace;
  private Watch watchClient;

  @Rule
  public Timeout timeout = Timeout.seconds(10);

  public WatchTest(boolean useNamespace) {
    this.useNamespace = useNamespace;
    this.watchClient = useNamespace ? clientWithNamespace.getWatchClient()
        : client.getWatchClient();
  }

  @Parameterized.Parameters(name = "UseNamespace = {0}")
  public static List<Boolean> parameters() {
    return Arrays.asList(new Boolean[] {true, false});
  }

  @BeforeClass
  public static void setUp() {
    client = Client.builder().endpoints(clusterResource.cluster().getClientEndpoints()).build();
    clientWithNamespace = Client.builder().endpoints(clusterResource.cluster()
        .getClientEndpoints()).namespace(namespace).build();
    kvClient = client.getKVClient();
  }

  @AfterClass
  public static void tearDown() {
    client.close();
    clientWithNamespace.close();
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

      putKV(key, value);
      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getEvents().size()).isEqualTo(1);
      assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
      assertThat(ref.get().getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    } finally {
      TestUtil.closeQuietly(watcher);
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

      putKV(key, value);
      latch.await(4, TimeUnit.SECONDS);

      assertThat(res).hasSize(2);
      assertThat(res.get(0)).isEqualToComparingFieldByFieldRecursively(res.get(1));
      assertThat(res.get(0).getEvents().size()).isEqualTo(1);
      assertThat(res.get(0).getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
      assertThat(res.get(0).getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    } finally {
      TestUtil.closeQuietly(w1);
      TestUtil.closeQuietly(w2);
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

      putKV(key, value);

      watcher = watchClient.watch(key, listener);
      deleteKey(key);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getEvents().size()).isEqualTo(1);

      WatchEvent event = ref.get().getEvents().get(0);
      assertThat(event.getEventType()).isEqualTo(EventType.DELETE);
      assertThat(Arrays.equals(event.getKeyValue().getKey().getBytes(), key.getBytes())).isTrue();
    } finally {
      TestUtil.closeQuietly(watcher);
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
      TestUtil.closeQuietly(watcher);
    }
  }

  private void putKV(ByteSequence key, ByteSequence value) throws Exception{
    if (useNamespace) {
      ByteSequence wKey = ByteSequence.from(namespace.getByteString().concat(key.getByteString()));
      kvClient.put(wKey, value).get();
    } else {
      kvClient.put(key, value).get();
    }
  }

  private void deleteKey(ByteSequence key) throws Exception {
    if (useNamespace) {
      ByteSequence wKey = ByteSequence.from(namespace.getByteString().concat(key.getByteString()));
      kvClient.delete(wKey).get();
    } else {
      kvClient.delete(key).get();
    }
  }
}
