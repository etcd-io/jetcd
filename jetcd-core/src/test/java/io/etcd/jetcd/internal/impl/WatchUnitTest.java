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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.api.Event.EventType;
import io.etcd.jetcd.api.KeyValue;
import io.etcd.jetcd.api.WatchGrpc.WatchImplBase;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.common.exception.ClosedWatcherException;
import io.etcd.jetcd.common.exception.CompactedException;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * watch test case.
 */
public class WatchUnitTest {

  private final static ByteSequence KEY = ByteSequence.from("test_key", UTF_8);
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
        .isThrownBy(() -> watchClient.watch(KEY));
  }

  @Test
  public void testWatchOnSendingWatchCreateRequest() {
    try (Watcher watcher = watchClient.watch(KEY, WatchOption.DEFAULT)) {
      // expects a WatchCreateRequest is created.
      verify(this.requestStreamObserverMock, timeout(100).times(1))
              .onNext(argThat(hasCreateKey(KEY)));
    }
  }

  @Test
  public void testWatcherListenOnResponse() throws InterruptedException {
    try (Watcher watcher = watchClient.watch(KEY, WatchOption.DEFAULT)) {
      WatchResponse createdResponse = WatchResponse.newBuilder()
              .setCreated(true)
              .setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse putResponse = WatchResponse
              .newBuilder()
              .setWatchId(0)
              .addEvents(Event.newBuilder().setType(EventType.PUT).build()).build();
      responseObserverRef.get().onNext(putResponse);

      io.etcd.jetcd.watch.WatchResponse actualResponse = watcher.listen();
      assertThat(actualResponse.getEvents().size()).isEqualTo(1);
      assertThat(actualResponse.getEvents().get(0).getEventType())
              .isEqualTo(WatchEvent.EventType.PUT);
    }
  }

  @Test
  public void testWatcherListenAfterWatcherClose() {
    Watcher watcher = watchClient.watch(KEY);
    watcher.close();

    assertThatExceptionOfType(ClosedWatcherException.class)
        .isThrownBy(watcher::listen);
  }

  @Test
  public void testWatcherListenOnWatcherClose() {
    Watcher watcher = watchClient.watch(KEY);

    executor.execute(() -> {
      try {
        Thread.sleep(20);
        watcher.close();
      } catch (InterruptedException e) {
        // do nothing
      }
    });

    assertThatExceptionOfType(ClosedWatcherException.class)
        .isThrownBy(watcher::listen);
  }

  @Test
  public void testWatcherListenOnWatchClientClose() throws InterruptedException {
    Watcher watcher = watchClient.watch(KEY);

    executor.execute(() -> {
      try {
        Thread.sleep(20);
        watchClient.close();
      } catch (InterruptedException e) {
        // do nothing
      }
    });

    assertThatExceptionOfType(ClosedClientException.class)
        .isThrownBy(watcher::listen);
  }

  @Test
  public void testWatcherListenForMultiplePuts() throws InterruptedException {
    try (Watcher watcher = watchClient.watch(KEY)) {
      WatchResponse createdResponse = WatchResponse.newBuilder()
              .setCreated(true)
              .setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse putResponse = WatchResponse
              .newBuilder()
              .setWatchId(0)
              .addEvents(Event.newBuilder()
                      .setType(EventType.PUT)
                      .setKv(KeyValue.newBuilder()
                              .setModRevision(2)
                              .build())
                      .build())
              .build();
      responseObserverRef.get().onNext(putResponse);

      io.etcd.jetcd.watch.WatchResponse actualResponse = watcher.listen();
      assertEqualOnWatchResponses(actualResponse,
              new io.etcd.jetcd.watch.WatchResponse(putResponse));

      putResponse = WatchResponse
              .newBuilder()
              .setWatchId(0)
              .addEvents(Event.newBuilder()
                      .setType(EventType.PUT)
                      .setKv(KeyValue.newBuilder()
                              .setModRevision(3)
                              .build())
                      .build())
              .build();
      responseObserverRef.get().onNext(putResponse);

      actualResponse = watcher.listen();
      assertEqualOnWatchResponses(actualResponse,
              new io.etcd.jetcd.watch.WatchResponse(putResponse));

    }
  }

  @Test
  public void testWatcherDelete() throws InterruptedException {
    try (Watcher watcher = watchClient.watch(KEY)) {
      WatchResponse createdResponse = WatchResponse
              .newBuilder()
              .setCreated(true)
              .setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse deleteResponse = WatchResponse
              .newBuilder()
              .setWatchId(0)
              .addEvents(Event
                      .newBuilder()
                      .setType(EventType.DELETE)
                      .build())
              .build();
      responseObserverRef.get().onNext(deleteResponse);

      io.etcd.jetcd.watch.WatchResponse actualResponse = watcher.listen();
      assertEqualOnWatchResponses(actualResponse,
              new io.etcd.jetcd.watch.WatchResponse(deleteResponse));
    }
  }

  @Test
  public void testWatchOnUnrecoverableConnectionIssue() {
    try (Watcher watcher = watchClient.watch(KEY, WatchOption.DEFAULT)) {
      WatchResponse createdResponse = WatchResponse.newBuilder().setCreated(true).setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      // connection error causes client to release all resources including all watchers.
      responseObserverRef.get()
              .onError(Status.ABORTED.withDescription("connection error").asRuntimeException());
      // expects connection error to propagate to active listener.

      assertThatExceptionOfType(EtcdException.class)
              .isThrownBy(watcher::listen)
              .withMessageContaining("connection error");
    }
  }

  @Test
  public void testWatchOnRecoverableConnectionIssue() {
    try (Watcher watcher = watchClient.watch(KEY, WatchOption.DEFAULT)) {
      WatchResponse createdResponse = WatchResponse.newBuilder()
              .setCreated(true)
              .setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);
      // expects a WatchCreateRequest is created.
      verify(this.requestStreamObserverMock, timeout(100).times(1))
              .onNext(argThat(hasCreateKey(KEY)));

      // connection error causes client to release all resources including all watchers.
      responseObserverRef.get()
              .onError(
                      Status.UNAVAILABLE.withDescription("Temporary connection issue").asRuntimeException());
      // resets mock call counter.
      Mockito.<StreamObserver>reset(this.requestStreamObserverMock);

      // expects re-send WatchCreateRequest.
      verify(this.requestStreamObserverMock, timeout(1000).times(1))
              .onNext(argThat(hasCreateKey(KEY)));
    }
  }

  @Test
  public void testWatcherCreateOnCompactionError() {
    try (Watcher watcher = watchClient.watch(KEY)) {
      WatchResponse createdResponse = WatchResponse
              .newBuilder()
              .setCreated(true)
              .setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse compactedResponse = WatchResponse
              .newBuilder()
              .setCompactRevision(2)
              .build();
      responseObserverRef.get().onNext(compactedResponse);

      assertThatThrownBy(watcher::listen)
              .isInstanceOf(CompactedException.class)
              .hasFieldOrPropertyWithValue("compactedRevision", 2L);
    }
  }

  @Test
  public void testWatcherCreateOnCancellationWithNoReason() {
    try (Watcher watcher = watchClient.watch(KEY)) {
      WatchResponse createdResponse = WatchResponse
              .newBuilder()
              .setCreated(true)
              .setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse canceledReponse = WatchResponse
              .newBuilder()
              .setCanceled(true)
              .build();
      responseObserverRef.get().onNext(canceledReponse);

      assertThatExceptionOfType(EtcdException.class)
              .isThrownBy(watcher::listen)
              .withMessageContaining("etcdserver: mvcc: required revision is a future revision");
    }
  }

  @Test
  public void testWatcherCreateOnCancellationWithReason() {
    try (Watcher watcher = watchClient.watch(KEY)) {
      WatchResponse createdResponse = WatchResponse
              .newBuilder()
              .setCreated(true)
              .setWatchId(0)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      WatchResponse canceledResponse = WatchResponse
              .newBuilder()
              .setCanceled(true)
              .setCancelReason("bad reason")
              .build();
      responseObserverRef.get().onNext(canceledResponse);

      assertThatExceptionOfType(EtcdException.class)
              .isThrownBy(watcher::listen)
              .withMessageContaining(canceledResponse.getCancelReason());
    }
  }

  @Test
  public void testWatcherCreateOnInvalidWatchID() {
    try (Watcher watcher = watchClient.watch(KEY)) {
      WatchResponse createdResponse = WatchResponse
              .newBuilder()
              .setCreated(true)
              .setWatchId(-1)
              .build();
      responseObserverRef.get().onNext(createdResponse);

      assertThatExceptionOfType(EtcdException.class)
              .isThrownBy(watcher::listen)
              .withMessageContaining("etcd server failed to create watch id");
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

  private static void assertEqualOnKeyValues(io.etcd.jetcd.data.KeyValue act,
      io.etcd.jetcd.data.KeyValue exp) {
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
}
