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

import static io.etcd.jetcd.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.api.Event.EventType;
import io.etcd.jetcd.api.WatchGrpc.WatchImplBase;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * watch test case.
 */
public class WatchUnitTest {

  private final static ByteSequence KEY = bytesOf("test_key");
  @Rule
  public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private Watch watchClient;
  private ExecutorService executor = Executors.newFixedThreadPool(2);
  private AtomicReference<StreamObserver<WatchResponse>> responseObserverRef;
  @Mock
  private StreamObserver<WatchRequest> requestStreamObserverMock;

  @Before
  public void setUp() throws IOException {
    this.executor = Executors.newSingleThreadExecutor();
    this.responseObserverRef = new AtomicReference<>();

    this.grpcServerRule.getServiceRegistry().addService(
        createWatchImpBase(responseObserverRef, requestStreamObserverMock));

    this.watchClient = new WatchImpl(
        new ClientConnectionManager(Client.builder(), this.grpcServerRule.getChannel())
    );
  }


  @After
  public void tearDown() throws Exception {
    watchClient.close();
    grpcServerRule.getChannel().shutdownNow();
    executor.shutdownNow();
  }

  @Test
  public void testCreateWatcherAfterClientClosed() {
    watchClient.close();

    assertThatExceptionOfType(ClosedClientException.class)
        .isThrownBy(() -> watchClient.watch(KEY, Watch.listener(r -> {})));
  }

  @Test
  public void testWatchOnSendingWatchCreateRequest() {
    try (Watch.Watcher watcher = watchClient.watch(KEY, WatchOption.DEFAULT, Watch.listener(r -> {}))) {
      // expects a WatchCreateRequest is created.
      verify(this.requestStreamObserverMock, timeout(100).times(1)).onNext(argThat(hasCreateKey(KEY)));
    }
  }

  @Test
  public void testWatcherListenOnResponse() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<io.etcd.jetcd.watch.WatchResponse> ref = new AtomicReference<>();
    Watch.Listener listener = Watch.listener(response -> {
      ref.set(response);
      latch.countDown();
    });

    try (Watch.Watcher watcher = watchClient.watch(KEY, WatchOption.DEFAULT, listener)) {
      WatchResponse createdResponse = createWatchResponse(0);
      responseObserverRef.get().onNext(createdResponse);

      io.etcd.jetcd.api.WatchResponse putResponse = io.etcd.jetcd.api.WatchResponse
              .newBuilder()
              .setWatchId(0)
              .addEvents(Event.newBuilder().setType(EventType.PUT).build()).build();
      responseObserverRef.get().onNext(putResponse);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get().getEvents().size()).isEqualTo(1);
      assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(WatchEvent.EventType.PUT);
    }
  }

  @Test
  public void testWatcherListenOnWatcherClose() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean ref = new AtomicBoolean();
    Watch.Listener listener = Watch.listener(
      r -> {},
      () -> {
        ref.set(true);
        latch.countDown();
      }
    );

    Watch.Watcher watcher = watchClient.watch(KEY, listener);

    executor.execute(() -> {
      try {
        Thread.sleep(20);
        watcher.close();
      } catch (InterruptedException e) {
        // do nothing
      }
    });

    latch.await(4, TimeUnit.SECONDS);

    assertThat(ref.get()).isTrue();
  }

  @Test
  public void testWatcherListenOnWatchClientClose() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean ref = new AtomicBoolean();
    Watch.Listener listener = Watch.listener(
      r -> {},
      () -> {
        ref.set(true);
        latch.countDown();
      }
    );

    Watch.Watcher watcher = watchClient.watch(KEY, listener);

    executor.execute(() -> {
      try {
        Thread.sleep(20);
        watchClient.close();
      } catch (InterruptedException e) {
        // do nothing
      }
    });

    latch.await(4, TimeUnit.SECONDS);

    assertThat(ref.get()).isTrue();
  }

  @Test
  public void testWatcherListenForMultiplePuts() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    List<io.etcd.jetcd.watch.WatchResponse> responses = new ArrayList<>();

    Watch.Listener listener = Watch.listener(
      r -> {
        responses.add(r);
        latch.countDown();
      }
    );

    try (Watch.Watcher watcher = watchClient.watch(KEY, listener)) {
      WatchResponse createdResponse = createWatchResponse(0);
      responseObserverRef.get().onNext(createdResponse);

      io.etcd.jetcd.api.WatchResponse resp1 = io.etcd.jetcd.api.WatchResponse.newBuilder()
        .setWatchId(0)
        .addEvents(Event.newBuilder()
          .setType(EventType.PUT)
          .setKv(io.etcd.jetcd.api.KeyValue.newBuilder()
            .setModRevision(2)
            .build())
          .build())
        .build();

      io.etcd.jetcd.api.WatchResponse resp2 = WatchResponse.newBuilder()
        .setWatchId(0)
        .addEvents(Event.newBuilder()
          .setType(EventType.PUT)
          .setKv(io.etcd.jetcd.api.KeyValue.newBuilder()
            .setModRevision(3)
            .build())
          .build())
        .build();

      responseObserverRef.get().onNext(resp1);
      responseObserverRef.get().onNext(resp2);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(responses).hasSize(2);
      assertEqualOnWatchResponses(responses.get(0), new io.etcd.jetcd.watch.WatchResponse(resp1));
      assertEqualOnWatchResponses(responses.get(1), new io.etcd.jetcd.watch.WatchResponse(resp2));
    }
  }

  @Test
  public void testWatcherDelete() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<io.etcd.jetcd.watch.WatchResponse> ref = new AtomicReference<>();
    Watch.Listener listener = Watch.listener(response -> {
      ref.set(response);
      latch.countDown();
    });

    try (Watch.Watcher watcher = watchClient.watch(KEY, listener)) {
      WatchResponse createdResponse = createWatchResponse(0);
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse deleteResponse = WatchResponse.newBuilder()
          .setWatchId(0)
          .addEvents(Event.newBuilder().setType(EventType.DELETE).build())
          .build();
      responseObserverRef.get().onNext(deleteResponse);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertEqualOnWatchResponses(ref.get(), new io.etcd.jetcd.watch.WatchResponse(deleteResponse));
    }
  }

  @Test
  public void testWatchOnUnrecoverableConnectionIssue() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ref = new AtomicReference<>();
    Watch.Listener listener = Watch.listener(
      r -> {},
      t -> {
      ref.set(t);
      latch.countDown();
    });

    try (Watch.Watcher watcher = watchClient.watch(KEY, WatchOption.DEFAULT, listener)) {
      WatchResponse createdResponse = createWatchResponse(0);

      responseObserverRef.get().onNext(createdResponse);
      responseObserverRef.get().onError(Status.ABORTED.withDescription("connection error").asRuntimeException());

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get()).isInstanceOf(EtcdException.class).hasMessageContaining("connection error");
    }
  }

  @Test
  public void testWatcherCreateOnCompactionError() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ref = new AtomicReference<>();
    Watch.Listener listener = Watch.listener(
      r -> {},
      t -> {
        ref.set(t);
        latch.countDown();
      }
    );

    try (Watch.Watcher watcher = watchClient.watch(KEY, listener)) {
      WatchResponse createdResponse = createWatchResponse(0);
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse compactedResponse = WatchResponse.newBuilder().setCanceled(true).setCompactRevision(2).build();
      responseObserverRef.get().onNext(compactedResponse);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get()).isInstanceOf(EtcdException.class).hasFieldOrPropertyWithValue("compactedRevision", 2L);
    }
  }

  @Test
  public void testWatcherCreateOnCancellationWithNoReason() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ref = new AtomicReference<>();
    Watch.Listener listener = Watch.listener(
      r -> {},
      t -> {
        ref.set(t);
        latch.countDown();
      }
    );

    try (Watch.Watcher watcher = watchClient.watch(KEY, listener)) {
      WatchResponse createdResponse = createWatchResponse(0);
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse canceledResponse = WatchResponse
              .newBuilder()
              .setCanceled(true)
              .build();
      responseObserverRef.get().onNext(canceledResponse);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get()).isInstanceOf(EtcdException.class)
        .hasMessageContaining("etcdserver: mvcc: required revision is a future revision");
    }
  }

  @Test
  public void testWatcherCreateOnCancellationWithReason() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ref = new AtomicReference<>();
    Watch.Listener listener = Watch.listener(
      r -> {},
      t -> {
        ref.set(t);
        latch.countDown();
      }
    );

    try (Watch.Watcher watcher = watchClient.watch(KEY, listener)) {
      WatchResponse createdResponse = createWatchResponse(0);
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse canceledResponse = WatchResponse
              .newBuilder()
              .setCanceled(true)
              .setCancelReason("bad reason")
              .build();
      responseObserverRef.get().onNext(canceledResponse);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get()).isInstanceOf(EtcdException.class)
        .hasMessageContaining(canceledResponse.getCancelReason());
    }
  }

  @Test
  public void testWatcherCreateOnInvalidWatchID() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ref = new AtomicReference<>();
    Watch.Listener listener = Watch.listener(
      r -> {},
      t -> {
        ref.set(t);
        latch.countDown();
      }
    );

    try (Watch.Watcher watcher = watchClient.watch(KEY, listener)) {
      WatchResponse createdResponse = createWatchResponse(-1);
      responseObserverRef.get().onNext(createdResponse);

      latch.await(4, TimeUnit.SECONDS);

      assertThat(ref.get()).isNotNull();
      assertThat(ref.get()).isInstanceOf(EtcdException.class)
        .hasMessageContaining("etcd server failed to create watch id");
    }
  }

  // return a ArgumentMatcher that checks if the captured WatchRequest has same key.
  private static ArgumentMatcher<WatchRequest> hasCreateKey(ByteSequence key) {
    return o -> Arrays.equals(o.getCreateRequest().getKey().toByteArray(), key.getBytes());
  }

  private static WatchImplBase createWatchImpBase(
      AtomicReference<StreamObserver<WatchResponse>> responseObserverRef,
      StreamObserver<WatchRequest> requestStreamObserver) {
    return new WatchImplBase() {
      @Override
      public StreamObserver<WatchRequest> watch(StreamObserver<WatchResponse> responseObserver) {
        responseObserverRef.set(responseObserver);
        return requestStreamObserver;
      }
    };
  }

  private static void assertEqualOnWatchResponses(io.etcd.jetcd.watch.WatchResponse expected,
      io.etcd.jetcd.watch.WatchResponse actual) {
    assertThat(actual.getEvents().size()).isEqualTo(expected.getEvents().size());

    for (int idx = 0; idx < expected.getEvents().size(); idx++) {
      WatchEvent act = actual.getEvents().get(idx);
      WatchEvent exp = actual.getEvents().get(idx);
      assertThat(act.getEventType()).isEqualTo(exp.getEventType());
      assertEqualOnKeyValues(act.getKeyValue(), exp.getKeyValue());
      assertEqualOnKeyValues(act.getPrevKV(), exp.getPrevKV());
    }
  }

  private static void assertEqualOnKeyValues(io.etcd.jetcd.KeyValue act, io.etcd.jetcd.KeyValue exp) {
    assertThat(act.getModRevision())
        .isEqualTo(exp.getModRevision());
    assertThat(act.getCreateRevision())
        .isEqualTo(exp.getCreateRevision());
    assertThat(act.getLease())
        .isEqualTo(exp.getLease());
    assertThat(act.getValue().getBytes())
        .isEqualTo(exp.getValue().getBytes());
    assertThat(act.getVersion())
        .isEqualTo(exp.getVersion());
  }

  private static WatchResponse createWatchResponse(int id, Event... events) {
    WatchResponse.Builder builder = WatchResponse.newBuilder()
      .setCreated(true)
      .setWatchId(id);

    for (Event event: events) {
      builder.addEvents(event);
    }

    return builder.build();
  }
}
