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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import com.google.protobuf.ByteString;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.api.MaintenanceGrpc.MaintenanceImplBase;
import io.etcd.jetcd.api.SnapshotRequest;
import io.etcd.jetcd.api.SnapshotResponse;
import io.etcd.jetcd.common.exception.EtcdException;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.util.MutableHandlerRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

// TODO: have separate folders to unit and integration tests.
public class MaintenanceUnitTest {

  @Rule
  public Timeout timeout = Timeout.seconds(10);

  private MutableHandlerRegistry serviceRegistry;
  private BlockingQueue<StreamObserver<SnapshotResponse>> observerQueue;
  private Server fakeServer;
  private ExecutorService executor;
  private Client client;
  private Maintenance maintenance;

  @Before
  public void setUp() throws IOException {
    observerQueue = new LinkedBlockingQueue<>();
    executor = Executors.newFixedThreadPool(2);

    serviceRegistry = new MutableHandlerRegistry();
    serviceRegistry.addService(new MaintenanceImplBase() {
       @Override
       public void snapshot(SnapshotRequest request, StreamObserver<SnapshotResponse> observer) {
         try {
           observerQueue.put(observer);
         } catch (InterruptedException e) {
           throw new RuntimeException(e);
         }
       }
     }
    );

    fakeServer = NettyServerBuilder.forPort(TestUtil.findNextAvailablePort())
        .fallbackHandlerRegistry(serviceRegistry)
        .directExecutor()
        .build()
        .start();

    client = Client.builder().endpoints("http://127.0.0.1:" + fakeServer.getPort()).build();
    maintenance = client.getMaintenanceClient();
  }

  @After
  public void tearDown() {
    maintenance.close();
    client.close();
    fakeServer.shutdownNow();
  }

  @Test
  public void testConnectionError() {
    executor.execute(() -> {
      try {
        Thread.sleep(50);
        observerQueue.take().onError(Status.ABORTED.asRuntimeException());
      } catch (InterruptedException e) {
        fail("expect no exception, but got InterruptedException", e);
      }
    });

    assertThatThrownBy(() -> maintenance.snapshot(NullOutputStream.NULL_OUTPUT_STREAM).get())
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(EtcdException.class);
  }

  @Test
  public void testWrite() throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteString blob = ByteString.copyFromUtf8("blob");
    final CompletableFuture<Long> answer = maintenance.snapshot(out);

    StreamObserver<SnapshotResponse> observer = observerQueue.take();
    observer.onNext(SnapshotResponse.newBuilder().setBlob(blob).setRemainingBytes(0).build());
    observer.onCompleted();

    answer.get();

    assertThat(out.toByteArray()).isEqualTo(blob.toByteArray());
  }
}

