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

import static org.assertj.core.api.Assertions.assertThat;

import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.launcher.junit5.EtcdClusterExtension;
import io.etcd.jetcd.watch.WatchEvent.EventType;
import io.etcd.jetcd.watch.WatchResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// TODO(#548): Add global timeout for tests once JUnit5 supports it
public class WatchResumeTest {

  @RegisterExtension
  public static final EtcdClusterExtension cluster = new EtcdClusterExtension("watch_resume", 3, false, true);

  private static Client client;
  private static Watch watchClient;
  private static KV kvClient;

  @BeforeAll
  public static void setUp() {
    client = Client.builder().endpoints(cluster.getClientEndpoints()).build();
    watchClient = client.getWatchClient();
    kvClient = client.getKVClient();
  }

  @AfterAll
  public static void tearDown() {
    client.close();
  }

  @Test
  public void testWatchOnPut() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    ByteSequence key = TestUtil.randomByteSequence();
    ByteSequence value = TestUtil.randomByteSequence();
    AtomicReference<WatchResponse> ref = new AtomicReference<>();

    try (Watcher watcher = watchClient.watch(key, response -> {
      ref.set(response);
      latch.countDown();
    })) {

      cluster.restart();

      kvClient.put(key, value).get(1, TimeUnit.SECONDS);
      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getEvents().size()).isEqualTo(1);
      assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
      assertThat(ref.get().getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    }
  }
}
