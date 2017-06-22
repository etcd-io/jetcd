package com.coreos.jetcd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.coreos.jetcd.Lease.KeepAliveListener;
import com.coreos.jetcd.api.LeaseGrpc.LeaseImplBase;
import com.coreos.jetcd.api.LeaseKeepAliveRequest;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

public class LeaseUnitTest {

  private Lease leaseCli;
  private AtomicReference<StreamObserver<LeaseKeepAliveResponse>> responseObserverRef;
  private static final long LEASE_ID = 1;
  private static final long LEASE_ID_2 = 2;

  @Rule
  public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public Timeout timeout = Timeout.seconds(10);

  @Mock
  private StreamObserver<LeaseKeepAliveRequest> requestStreamObserverMock;

  @Before
  public void setup() throws IOException, AuthFailedException, ConnectException {
    this.responseObserverRef = new AtomicReference<>();
    this.grpcServerRule.getServiceRegistry().addService(
        this.createLeaseImplBase(this.responseObserverRef, this.requestStreamObserverMock));

    this.leaseCli = new LeaseImpl(this.grpcServerRule.getChannel(), Optional.empty(),
        Executors.newCachedThreadPool());
  }

  @After
  public void tearDown() throws InterruptedException {
    this.leaseCli.close();
    this.grpcServerRule.getServer().shutdownNow();
  }

  @Test
  public void testKeepAliveOnce() throws ExecutionException, InterruptedException {
    CompletableFuture<com.coreos.jetcd.lease.LeaseKeepAliveResponse> lrpFuture = this.leaseCli
        .keepAliveOnce(LEASE_ID);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID)
        .build();
    this.responseObserverRef.get().onNext(lrp);

    com.coreos.jetcd.lease.LeaseKeepAliveResponse lrpActual = lrpFuture.get();
    assertThat(lrpActual.getID()).isEqualTo(lrp.getID());
  }

  @Test
  public void testKeepAliveOnceConnectError() throws ExecutionException, InterruptedException {
    CompletableFuture<com.coreos.jetcd.lease.LeaseKeepAliveResponse> lrpFuture = this.leaseCli
        .keepAliveOnce(LEASE_ID);
    Throwable t = Status.ABORTED.asRuntimeException();
    responseObserverRef.get().onError(t);

    assertThatThrownBy(() -> lrpFuture.get()).hasCause(t);
  }

  // TODO: sometime this.responseObserverRef.get().onNext(lrp) blocks even though client has received msg;
  // seems like a bug in grpc test framework.
  public void testKeepAliveOnceStreamCloseOnSuccess()
      throws ExecutionException, InterruptedException {
    CompletableFuture<com.coreos.jetcd.lease.LeaseKeepAliveResponse> lrpFuture = this.leaseCli
        .keepAliveOnce(LEASE_ID);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID)
        .build();
    this.responseObserverRef.get().onNext(lrp);

    lrpFuture.get();
    verify(this.requestStreamObserverMock, timeout(100).times(1)).onCompleted();
  }

  // TODO: sometime this.responseObserverRef.get().onNext(lrp) blocks even though client has received msg;
  // seems like a bug in grpc test framework.
  public void testKeepAliveListenOnOneResponse() throws InterruptedException {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID)
        .setTTL(2)
        .build();
    System.out.println(System.currentTimeMillis() + " responseObserverRef.onNext() ");
    this.responseObserverRef.get().onNext(lrp);

    com.coreos.jetcd.lease.LeaseKeepAliveResponse actual = listener.listen();
    assertThat(actual.getID()).isEqualTo(lrp.getID());
    assertThat(actual.getTTL()).isEqualTo(lrp.getTTL());

    listener.close();
  }

  @Test
  public void testKeepAliveListenAfterListenerClose() {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    listener.close();
    assertThatThrownBy(() -> listener.listen())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("KeepAliveListener has closed");
  }

  @Test
  public void testKeepAliveListenerClosesOnListening()
      throws ExecutionException, InterruptedException {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    Future<?> donef = Executors.newCachedThreadPool().submit(() -> {
      Thread.sleep(50);
      listener.close();
      return 1;
    });
    assertThatThrownBy(() -> listener.listen()).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("KeepAliveListener encounters error on listen");
    donef.get();
  }

  @Test
  public void testKeepAliveClientClosesOnListening()
      throws ExecutionException, InterruptedException {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    Future<?> donef = Executors.newCachedThreadPool().submit(() -> {
      Thread.sleep(50);
      this.leaseCli.close();
      return 1;
    });

    assertThatThrownBy(() -> listener.listen())
        .hasCause(new IllegalStateException("Lease client has closed"));
    donef.get();
  }

  @Test
  public void testKeepAliveOnSendingKeepAliveRequests() {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    // expect more than one KeepAlive requests are sent within 1100 ms.
    verify(this.requestStreamObserverMock, timeout(1100).atLeast(2))
        .onNext(argThat(hasLeaseID(LEASE_ID)));
    listener.close();
  }

  @Test
  public void testKeepAliveAfterFirstKeepAliveTimeout() throws InterruptedException {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    // expect at least some KeepAlive requests are sent within
    // firstKeepAliveTimeout(5000 ms) + some quiet time (1000 ms).
    verify(this.requestStreamObserverMock, after(6000).atLeastOnce())
        .onNext(argThat(hasLeaseID(LEASE_ID)));
    // reset mock to a empty state.
    reset(this.requestStreamObserverMock);
    // verify no keepAlive requests are sent within one second after firstKeepAliveTimeout.
    verify(this.requestStreamObserverMock, after(1000).times(0))
        .onNext(argThat(hasLeaseID(LEASE_ID)));

    listener.close();
  }

  @Test
  public void testTimeToLiveNullOption() throws ExecutionException, InterruptedException {
    assertThatThrownBy(() -> this.leaseCli.timeToLive(LEASE_ID, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("LeaseOption should not be null");
  }

  @Test
  public void testKeepAliveCloseOnlyListener() {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    listener.close();
    // expect no more keep alive requests are sent after the initial one
    // within 1 second after closing the only listener.
    verify(this.requestStreamObserverMock, after(1000).atMost(1))
        .onNext(argThat(hasLeaseID(LEASE_ID)));
  }

  @Test
  public void testKeepAliveCloseSomeListeners() {
    KeepAliveListener closingListener = this.leaseCli.keepAlive(LEASE_ID_2);
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    closingListener.close();
    // expect closing closingListener doesn't affect sending keep alive requests for LEASE_ID_2.
    verify(this.requestStreamObserverMock, after(1200).atLeast(2))
        .onNext(argThat(hasLeaseID(LEASE_ID)));

    listener.close();
  }

  // TODO: sometime this.responseObserverRef.get().onNext(lrp) blocks even though client has received msg;
  // seems like a bug in grpc test framework.
  public void testKeepAliveReceivesExpiredLease() {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID)
        .setTTL(0)
        .build();
    this.responseObserverRef.get().onNext(lrp);

    // expect lease expired exception.
    assertThatThrownBy(() -> listener.listen())
        .hasCause(new IllegalStateException("Lease " + LEASE_ID + " expired"));

    // expect no more keep alive requests for LEASE_ID after receiving lease expired response.
    verify(this.requestStreamObserverMock, after(1000).atMost(1))
        .onNext(argThat(hasLeaseID(LEASE_ID)));

    listener.close();
  }

  @Test
  public void testKeepAliveResetOnStreamErrors() {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    Throwable t = Status.ABORTED.asRuntimeException();
    // onError triggers reset() to be executed.
    // scheduler will execute reset() in 500 ms.
    responseObserverRef.get().onError(t);

    // expect keep alive requests are still sending even with reset.
    verify(this.requestStreamObserverMock, timeout(2000).atLeast(3))
        .onNext(argThat(hasLeaseID(LEASE_ID)));

    listener.close();
  }

  // return a ArgumentMatcher that checks if the captured LeaseKeepAliveRequest has same leaseId.
  private ArgumentMatcher<LeaseKeepAliveRequest> hasLeaseID(long leaseId) {
    return new ArgumentMatcher<LeaseKeepAliveRequest>() {
      @Override
      public boolean matches(Object o) {
        if (!(o instanceof LeaseKeepAliveRequest)) {
          return false;
        }

        return ((LeaseKeepAliveRequest) o).getID() == leaseId;
      }
    };
  }

  private LeaseImplBase createLeaseImplBase(
      AtomicReference<StreamObserver<LeaseKeepAliveResponse>> responseObserverRef,
      StreamObserver<LeaseKeepAliveRequest> requestStreamObserver) {
    return new LeaseImplBase() {
      @Override
      public StreamObserver<LeaseKeepAliveRequest> leaseKeepAlive(
          StreamObserver<LeaseKeepAliveResponse> responseObserver) {
        responseObserverRef.set(responseObserver);
        return requestStreamObserver;
      }
    };
  }
}
