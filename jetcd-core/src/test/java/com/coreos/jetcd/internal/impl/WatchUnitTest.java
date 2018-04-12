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

package com.coreos.jetcd.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.Watch.Watcher;
import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.api.Event.EventType;
import com.coreos.jetcd.api.KeyValue;
import com.coreos.jetcd.api.WatchGrpc.WatchImplBase;
import com.coreos.jetcd.api.WatchRequest;
import com.coreos.jetcd.api.WatchResponse;
import com.coreos.jetcd.common.exception.ClosedClientException;
import com.coreos.jetcd.common.exception.ClosedWatcherException;
import com.coreos.jetcd.common.exception.CompactedException;
import com.coreos.jetcd.common.exception.EtcdException;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
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
  private static final ByteSequence KEY = ByteSequence.fromString("test_key");

  @Rule
  public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public final Timeout timeout = Timeout.seconds(10);

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

      com.coreos.jetcd.watch.WatchResponse actualResponse = watcher.listen();
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

      com.coreos.jetcd.watch.WatchResponse actualResponse = watcher.listen();
      assertEqualOnWatchResponses(actualResponse,
              new com.coreos.jetcd.watch.WatchResponse(putResponse));

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
              new com.coreos.jetcd.watch.WatchResponse(putResponse));

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

      com.coreos.jetcd.watch.WatchResponse actualResponse = watcher.listen();
      assertEqualOnWatchResponses(actualResponse,
              new com.coreos.jetcd.watch.WatchResponse(deleteResponse));
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
  private ArgumentMatcher<WatchRequest> hasCreateKey(ByteSequence key) {
    return o -> Arrays.equals(o.getCreateRequest().getKey().toByteArray(), key.getBytes());
  }

  private WatchImplBase createWatchImpBase(
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

  private void assertEqualOnWatchResponses(com.coreos.jetcd.watch.WatchResponse expected,
      com.coreos.jetcd.watch.WatchResponse actual) {
    assertThat(actual.getEvents().size()).isEqualTo(expected.getEvents().size());

    for (int idx = 0; idx < expected.getEvents().size(); idx++) {
      WatchEvent act = actual.getEvents().get(idx);
      WatchEvent exp = actual.getEvents().get(idx);
      assertThat(act.getEventType()).isEqualTo(exp.getEventType());
      assertEqualOnKeyValues(act.getKeyValue(), exp.getKeyValue());
      assertEqualOnKeyValues(act.getPrevKV(), exp.getPrevKV());
    }
  }

  private void assertEqualOnKeyValues(com.coreos.jetcd.data.KeyValue act,
      com.coreos.jetcd.data.KeyValue exp) {
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
