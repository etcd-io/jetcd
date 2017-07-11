package com.coreos.jetcd.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.Maintenance.Snapshot;
import com.coreos.jetcd.api.MaintenanceGrpc.MaintenanceImplBase;
import com.coreos.jetcd.api.SnapshotRequest;
import com.coreos.jetcd.api.SnapshotResponse;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.util.MutableHandlerRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

// TODO: have separate folders to unit and integration tests.
public class MaintenanceUnitTest {

  private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();
  private Server fakeServer;
  private ExecutorService executor = Executors.newFixedThreadPool(2);
  private Maintenance maintenanceCli;
  private final AtomicReference<StreamObserver<SnapshotResponse>> responseObserverRef = new AtomicReference<>();

  @BeforeTest
  public void setUp() throws IOException {
    String uniqueServerName = "fake server for " + getClass();

    fakeServer = InProcessServerBuilder.forName(uniqueServerName)
        .fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start();

    ManagedChannelBuilder channelBuilder = InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
    ClientBuilder clientBuilder = ClientBuilder.newBuilder().setChannelBuilder(channelBuilder).setEndpoints("127.0.0.1:2379");
    maintenanceCli = new ClientImpl(clientBuilder).getMaintenanceClient();

    MaintenanceImplBase base = this.defaultBase(responseObserverRef);
    serviceRegistry.addService(base);
  }

  @AfterTest
  public void tearDown() {
    fakeServer.shutdownNow();
  }

  @Test(timeOut = 1000)
  public void testConnectionError() throws IOException {
    Snapshot snapshot = maintenanceCli.snapshot();
    OutputStream out = new ByteArrayOutputStream();
    executor.execute(() -> {
      try {
        Thread.sleep(50);
        responseObserverRef.get()
            .onError(Status.ABORTED.asRuntimeException());
      } catch (InterruptedException e) {
        Assert.fail("expect no exception, but got InterruptedException", e);
      }
    });

    assertThatThrownBy(() -> snapshot.write(out))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("connection error");
  }

  @Test(timeOut = 1000)
  public void testWriteAfterClosed() throws IOException {
    Snapshot snapshot = maintenanceCli.snapshot();
    snapshot.close();
    OutputStream out = new ByteArrayOutputStream();
    assertThatThrownBy(() -> snapshot.write(out))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Snapshot has closed");
  }

  @Test(timeOut = 1000)
  public void testWriteTwice() throws IOException {
    Snapshot snapshot = maintenanceCli.snapshot();
    responseObserverRef.get().onCompleted();
    OutputStream out = new ByteArrayOutputStream();
    snapshot.write(out);
    assertThatThrownBy(() -> snapshot.write(out))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("write is called more than once");
  }

  @Test(timeOut = 1000)
  public void testCloseWhenWrite() throws IOException {
    Snapshot snapshot = maintenanceCli.snapshot();
    OutputStream out = new ByteArrayOutputStream();
    executor.execute(() -> {
      try {
        Thread.sleep(50);
        snapshot.close();
      } catch (Exception e) {
        Assert.fail("don't expect any exception, but got", e);
      }
    });
    assertThatThrownBy(() -> snapshot.write(out))
        .isInstanceOf(IOException.class);
  }

  @Test(timeOut = 1000)
  public void testInterruptWrite() throws ExecutionException, InterruptedException {
    Snapshot snapshot = maintenanceCli.snapshot();
    OutputStream out = new ByteArrayOutputStream();
    Future<?> done = executor.submit(() ->
        assertThatThrownBy(() -> snapshot.write(out))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("write is interrupted"));
    Thread.sleep(50);
    executor.shutdownNow();
    done.get();
  }

  @Test(timeOut = 1000)
  public void testWrite() throws IOException {
    Snapshot snapshot = maintenanceCli.snapshot();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteString blob = ByteString.copyFromUtf8("blob");

    responseObserverRef.get().onNext(SnapshotResponse.newBuilder()
        .setBlob(blob)
        .setRemainingBytes(0)
        .build());
    responseObserverRef.get().onCompleted();
    snapshot.write(out);
    assertThat(out.toByteArray()).isEqualTo(blob.toByteArray());
  }

  public MaintenanceImplBase defaultBase(
      AtomicReference<StreamObserver<SnapshotResponse>> responseObserverRef) {
    return new MaintenanceImplBase() {
      @Override
      public void snapshot(SnapshotRequest request,
          StreamObserver<SnapshotResponse> responseObserver) {
        responseObserverRef.set(responseObserver);
        // do nothing.
      }
    };
  }
}

