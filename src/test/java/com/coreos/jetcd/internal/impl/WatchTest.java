package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * watch test case.
 */
public class WatchTest {

  private Client client;
  private Watch watchClient;
  private KV kvClient;
  private BlockingQueue<WatchEvent> eventsQueue = new LinkedBlockingDeque<>();

  private ByteSequence key = ByteSequence.fromString("test_key");
  private ByteSequence value = ByteSequence.fromString("test_val");
  private WatchImpl.Watcher watcher;

  private Assertion test = new Assertion();

  @BeforeTest
  public void newEtcdClient() throws AuthFailedException, ConnectException {
    client = ClientBuilder.newBuilder().endpoints("localhost:2379").build();
    watchClient = client.getWatchClient();
    kvClient = client.getKVClient();
  }

  @Test
  public void testWatch() throws ExecutionException, InterruptedException {
    WatchOption option = WatchOption.DEFAULT;
    watcher = watchClient.watch(key, option, new Watch.WatchCallback() {

      /**
       * onWatch will be called when watcher receive any events
       *
       * @param header
       * @param events received events
       */
      @Override
      public void onWatch(Header header, List<WatchEvent> events) {
        WatchTest.this.eventsQueue.addAll(events);
      }

      /**
       * onResuming will be called when the watcher is on resuming.
       */
      @Override
      public void onResuming() {

      }

    }).get();

  }

  /**
   * watch put operation on key
   * assert whether receive put event
   */
  @Test(dependsOnMethods = "testWatch")
  public void testWatchPut() throws InterruptedException {
    kvClient
        .put(key, value);
    WatchEvent event = eventsQueue.poll(5, TimeUnit.SECONDS);
    test.assertEquals(event.getKeyValue().getKey(), key);
    test.assertEquals(event.getEventType(), WatchEvent.EventType.PUT);
  }

  /**
   * watch delete operation on key
   * assert whether receive delete event
   */
  @Test(dependsOnMethods = "testWatchPut")
  public void testWatchDelete() throws InterruptedException {
    kvClient.delete(key);
    WatchEvent event = eventsQueue.poll(5, TimeUnit.SECONDS);
    test.assertEquals(event.getKeyValue().getKey(), key);
    test.assertEquals(event.getEventType(), WatchEvent.EventType.DELETE);
  }

  /**
   * cancel watch test case
   * assert whether receive cancel response
   */
  @Test(dependsOnMethods = "testWatchDelete")
  public void testCancelWatch() throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<Boolean> future = watcher.cancel();
    test.assertTrue(future.get(5, TimeUnit.SECONDS));
  }
}
