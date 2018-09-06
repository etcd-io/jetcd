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
package io.etcd.jetcd.internal.impl;

import static com.google.common.base.Charsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.launcher.junit.EtcdClusterResource;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchEvent.EventType;
import io.etcd.jetcd.watch.WatchResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
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
  public static void tearDown() throws IOException {
    client.close();
  }

  @Test
  public void testWatchOnPut() throws ExecutionException, InterruptedException {
    ByteSequence key = TestUtil.randomByteSequence();
    ByteSequence value = ByteSequence.from("value", UTF_8);
    try (Watcher watcher = watchClient.watch(key)) {
      kvClient.put(key, value).get();

      WatchResponse response = watcher.listen();
      assertThat(response.getEvents().size()).isEqualTo(1);
      assertThat(response.getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
      assertThat(response.getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    }
  }

  @Test
  public void testWatchOnDelete() throws ExecutionException, InterruptedException {
    ByteSequence key = TestUtil.randomByteSequence();
    ByteSequence value = ByteSequence.from("value", UTF_8);
    kvClient.put(key, value).get();
    try (Watcher watcher = watchClient.watch(key)) {
      kvClient.delete(key).get();
      WatchResponse response = watcher.listen();
      assertThat(response.getEvents().size()).isEqualTo(1);
      WatchEvent event = response.getEvents().get(0);
      assertThat(event.getEventType()).isEqualTo(EventType.DELETE);
      assertThat(Arrays.equals(event.getKeyValue().getKey().getBytes(), key.getBytes())).isTrue();
    }
  }
}
