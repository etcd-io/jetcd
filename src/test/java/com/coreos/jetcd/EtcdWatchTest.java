package com.coreos.jetcd;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.EtcdHeader;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

import java.util.List;
import java.util.concurrent.*;

/**
 * watch test case.
 */
public class EtcdWatchTest {

    private EtcdClient client;
    private EtcdWatch watchClient;
    private EtcdKV kvClient;
    private BlockingQueue<WatchEvent> eventsQueue = new LinkedBlockingDeque<>();

    private ByteSequence key = ByteSequence.fromString("test_key");
    private ByteSequence value = ByteSequence.fromString("test_val");
    private EtcdWatchImpl.Watcher watcher;

    private Assertion test = new Assertion();

    @BeforeTest
    public void newEtcdClient() throws AuthFailedException, ConnectException {
        client = EtcdClientBuilder.newBuilder().endpoints("localhost:2379").build();
        watchClient = client.getWatchClient();
        kvClient = client.getKVClient();
    }

    @Test
    public void testWatch() throws ExecutionException, InterruptedException {
        WatchOption option = WatchOption.DEFAULT;
        watcher = watchClient.watch(key, option, new EtcdWatch.WatchCallback() {

            /**
             * onWatch will be called when watcher receive any events
             *
             * @param header
             * @param events received events
             */
            @Override
            public void onWatch(EtcdHeader header, List<WatchEvent> events) {
                EtcdWatchTest.this.eventsQueue.addAll(events);
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
        kvClient.put(EtcdUtil.byteStringFromByteSequence(key), EtcdUtil.byteStringFromByteSequence(value));
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
        kvClient.delete(EtcdUtil.byteStringFromByteSequence(key));
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
