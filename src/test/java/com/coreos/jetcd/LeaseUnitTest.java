package com.coreos.jetcd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.coreos.jetcd.api.LeaseGrpc.LeaseImplBase;
import com.coreos.jetcd.api.LeaseKeepAliveRequest;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.options.LeaseOption;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


public class LeaseUnitTest {

  private Lease leaseCli;
  private AtomicReference<StreamObserver<LeaseKeepAliveResponse>> responseObserverRef;
  private static final long LEASE_ID = 1;

  @Rule
  public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private StreamObserver<LeaseKeepAliveRequest> requestStreamObserverMock;

  @Before
  public void setup() throws IOException, AuthFailedException, ConnectException {
    this.responseObserverRef = new AtomicReference<>();
    this.grpcServerRule.getServiceRegistry().addService(
        this.createLeaseImplBase(this.responseObserverRef, this.requestStreamObserverMock));
    this.leaseCli = new LeaseImpl(this.grpcServerRule.getChannel(), Optional.empty());
  }

  @After
  public void tearDown() throws InterruptedException {
    this.grpcServerRule.getServer().shutdownNow();
  }

  @Test(timeout = 1000)
  public void testKeepAliveOnce() throws ExecutionException, InterruptedException {
    CompletableFuture<LeaseKeepAliveResponse> lrpFuture = this.leaseCli.keepAliveOnce(LEASE_ID);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID)
        .build();
    this.responseObserverRef.get().onNext(lrp);

    LeaseKeepAliveResponse lrpActual = lrpFuture.get();
    assertThat(lrpActual).isEqualTo(lrp);
  }

  @Test(timeout = 1000)
  public void testKeepAliveOnceConnectError() throws ExecutionException, InterruptedException {
    CompletableFuture<LeaseKeepAliveResponse> lrpFuture = this.leaseCli.keepAliveOnce(LEASE_ID);
    Throwable t = Status.ABORTED.asRuntimeException();
    responseObserverRef.get().onError(t);
    
    assertThatThrownBy(() -> lrpFuture.get()).hasCause(t);
  }

  @Test(timeout = 1000)
  public void testKeepAliveOnceStreamCloseOnSuccess()
      throws ExecutionException, InterruptedException {
    CompletableFuture<LeaseKeepAliveResponse> lrpFuture = this.leaseCli.keepAliveOnce(LEASE_ID);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID)
        .build();
    this.responseObserverRef.get().onNext(lrp);

    lrpFuture.get();
    verify(this.requestStreamObserverMock, times(1)).onCompleted();
  }

  @Test
  public void testTimeToLiveNullOption() throws ExecutionException, InterruptedException {
    assertThatThrownBy(() -> this.leaseCli.timeToLive(LEASE_ID, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("LeaseOption should not be null");
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
