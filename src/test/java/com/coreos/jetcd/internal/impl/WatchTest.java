package com.coreos.jetcd.internal.impl;


import static org.assertj.core.api.Assertions.assertThat;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.Watch.Watcher;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchEvent.EventType;
import com.coreos.jetcd.watch.WatchResponse;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * watch test case.
 */
public class WatchTest {

  private Client client;
  private Watch watchClient;
  private KV kvClient;

  @Rule
  public Timeout timeout = Timeout.seconds(10);

  @Before
  public void setUp() {
    client = ClientBuilder.newBuilder().setEndpoints(TestConstants.endpoints).build();
    watchClient = client.getWatchClient();
    kvClient = client.getKVClient();
  }

  @After
  public void tearDown() {
    client.close();
  }

  @Test
  public void testWatchOnPut() throws ExecutionException, InterruptedException {
    ByteSequence key = ByteSequence.fromString(TestUtil.randomString());
    ByteSequence value = ByteSequence.fromString("value");
    Watcher watcher = watchClient.watch(key);
    try {
      kvClient.put(key, value).get();

      WatchResponse response = watcher.listen();
      assertThat(response.getEvents().size()).isEqualTo(1);
      assertThat(response.getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
      assertThat(response.getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
    } finally {
      watcher.close();
    }
  }

  @Test
  public void testWatchOnDelete() throws ExecutionException, InterruptedException {
    ByteSequence key = ByteSequence.fromString(TestUtil.randomString());
    ByteSequence value = ByteSequence.fromString("value");
    kvClient.put(key, value).get();
    Watcher watcher = watchClient.watch(key);
    try {
      kvClient.delete(key);
      WatchResponse response = watcher.listen();
      assertThat(response.getEvents().size()).isEqualTo(1);
      WatchEvent event = response.getEvents().get(0);
      assertThat(event.getEventType()).isEqualTo(EventType.DELETE);
      assertThat(Arrays.equals(event.getKeyValue().getKey().getBytes(), key.getBytes())).isTrue();
    } finally {
      watcher.close();
    }
  }
}



