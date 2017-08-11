package com.coreos.jetcd.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.Maintenance.Snapshot;
import com.coreos.jetcd.api.MaintenanceGrpc.MaintenanceImplBase;
import com.coreos.jetcd.api.SnapshotRequest;
import com.coreos.jetcd.api.SnapshotResponse;
import com.coreos.jetcd.exception.ClosedSnapshotException;
import com.coreos.jetcd.exception.EtcdException;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.util.MutableHandlerRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.testng.Assert;

// TODO: have separate folders to unit and integration tests.
public class MaintenanceUnitTest {

  private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();
  private final BlockingQueue<StreamObserver<SnapshotResponse>> observerQueue = new LinkedBlockingQueue<>();
  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private Server fakeServer;
  private ExecutorService executor = Executors.newFixedThreadPool(2);
  private Client client;
  private Maintenance maintenance;

  @Before
  public void setUp() throws IOException {
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
  public void testConnectionError() throws Exception {
    final Snapshot snapshot = maintenance.snapshot();
    final OutputStream out = new ByteArrayOutputStream();

    executor.execute(() -> {
      try {
        Thread.sleep(50);
        observerQueue.take().onError(Status.ABORTED.asRuntimeException());
      } catch (InterruptedException e) {
        Assert.fail("expect no exception, but got InterruptedException", e);
      }
    });

    assertThatThrownBy(() -> snapshot.write(out))
        .isInstanceOf(IOException.class)
        .hasCauseInstanceOf(EtcdException.class);
  }

  @Test
  public void testWriteAfterClosed() throws Exception {
    Snapshot snapshot = maintenance.snapshot();
    snapshot.close();
    OutputStream out = new ByteArrayOutputStream();
    assertThatExceptionOfType(ClosedSnapshotException.class)
        .isThrownBy(() -> snapshot.write(out));
  }

  @Test
  public void testWriteTwice() throws Exception {
    Snapshot snapshot = maintenance.snapshot();
    observerQueue.take().onCompleted();
    OutputStream out = new ByteArrayOutputStream();
    snapshot.write(out);
    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> snapshot.write(out))
        .withMessageContaining("write is called more than once");
  }

  @Test
  public void testCloseWhenWrite() throws Exception {
    final Snapshot snapshot = maintenance.snapshot();
    final OutputStream out = new ByteArrayOutputStream();

    executor.execute(() -> {
      try {
        Thread.sleep(50);
        snapshot.close();
      } catch (Exception e) {
        Assert.fail("don't expect any exception, but got", e);
      }
    });
    assertThatExceptionOfType(ClosedSnapshotException.class)
        .isThrownBy(() -> snapshot.write(out));
  }

  @Test
  public void testInterruptWrite() throws ExecutionException, InterruptedException {
    final Snapshot snapshot = maintenance.snapshot();
    final OutputStream out = new ByteArrayOutputStream();
    Future<?> done = executor.submit(
        () ->
            assertThatExceptionOfType(InterruptedException.class)
                .isThrownBy(() -> snapshot.write(out))
    );
    Thread.sleep(50);
    executor.shutdownNow();
    done.get();
  }

  @Test
  public void testWrite() throws Exception {
    final Snapshot snapshot = maintenance.snapshot();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteString blob = ByteString.copyFromUtf8("blob");

    StreamObserver<SnapshotResponse> observer = observerQueue.take();

    observer.onNext(SnapshotResponse.newBuilder()
        .setBlob(blob)
        .setRemainingBytes(0)
        .build());
    observer.onCompleted();

    snapshot.write(out);

    assertThat(out.toByteArray()).isEqualTo(blob.toByteArray());
  }
}

