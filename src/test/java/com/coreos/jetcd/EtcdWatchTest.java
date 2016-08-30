package com.coreos.jetcd;

import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.api.WatchResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.util.ListenableSetFuture;
import com.coreos.jetcd.watch.Watcher;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
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
    private BlockingQueue<Event> eventsQueue = new LinkedBlockingDeque<>();

    private ByteString key = ByteString.copyFromUtf8("test_key");
    private ByteString value = ByteString.copyFromUtf8("test_val");
    private Watcher watcher;
    private ListenableSetFuture<WatchResponse> cancelResponse;

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
        cancelResponse = new ListenableSetFuture<>(null);
        watcher = watchClient.watch(key, option, new Watcher.WatchCallback() {
            @Override
            public void onWatch(List<Event> events) {
                EtcdWatchTest.this.eventsQueue.addAll(events);
            }

            @Override
            public void onCreateFailed(WatchResponse watchResponse) {

            }

            @Override
            public void onResuming() {

            }

            @Override
            public void onCanceled(WatchResponse response) {
                cancelResponse.setResult(response);
            }
        }).get();

    }

    /**
     * watch put operation on key
     * assert whether receive put event
     */
    @Test(dependsOnMethods = "testWatch")
    public void testWatchPut() throws InterruptedException {
        kvClient.put(key, value);
        Event event = eventsQueue.poll(5, TimeUnit.SECONDS);
        test.assertEquals(event.getKv().getKey(), key);
        test.assertEquals(event.getType(), Event.EventType.PUT);
    }

    /**
     * watch delete operation on key
     * assert whether receive delete event
     */
    @Test(dependsOnMethods = "testWatchPut")
    public void testWatchDelete() throws InterruptedException {
        kvClient.delete(key);
        Event event = eventsQueue.poll(5, TimeUnit.SECONDS);
        test.assertEquals(event.getKv().getKey(), key);
        test.assertEquals(event.getType(), Event.EventType.DELETE);
    }

    /**
     * cancel watch test case
     * assert whether receive cancel response
     */
    @Test(dependsOnMethods = "testWatchDelete")
    public void testCancelWatch() throws ExecutionException, InterruptedException, TimeoutException {
        watchClient.cancelWatch(watcher);
        WatchResponse watchResponse = cancelResponse.get(5, TimeUnit.SECONDS);
        test.assertTrue(watchResponse.getCanceled());
    }
}
