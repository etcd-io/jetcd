/*
 * Copyright 2016-2021 The jetcd authors
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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.etcd.jetcd.api.LeaseGrpc.LeaseImplBase;
import io.etcd.jetcd.api.LeaseKeepAliveRequest;
import io.etcd.jetcd.api.LeaseKeepAliveResponse;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.support.Observers;
import io.etcd.jetcd.test.GrpcServerExtension;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Timeout(value = 1, unit = TimeUnit.MINUTES)
@ExtendWith(MockitoExtension.class)
public class LeaseUnitTest {

    private Lease leaseCli;
    private AtomicReference<StreamObserver<LeaseKeepAliveResponse>> responseObserverRef;
    private static final long LEASE_ID_1 = 1;
    private static final long LEASE_ID_2 = 2;

    @RegisterExtension
    public final GrpcServerExtension grpcServerRule = new GrpcServerExtension().directExecutor();

    @Mock
    private StreamObserver<LeaseKeepAliveRequest> requestStreamObserverMock;

    @BeforeEach
    public void setup() throws IOException {
        this.responseObserverRef = new AtomicReference<>();
        this.grpcServerRule.getServiceRegistry()
            .addService(this.createLeaseImplBase(this.responseObserverRef, this.requestStreamObserverMock));

        this.leaseCli = new LeaseImpl(new ClientConnectionManager(Client.builder(), grpcServerRule.getChannel()));
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        this.leaseCli.close();
        this.grpcServerRule.getServer().shutdownNow();
    }

    @Test
    public void testKeepAliveOnce() throws ExecutionException, InterruptedException {
        CompletableFuture<io.etcd.jetcd.lease.LeaseKeepAliveResponse> lrpFuture = this.leaseCli.keepAliveOnce(LEASE_ID_1);
        LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse.newBuilder().setID(LEASE_ID_1).build();
        this.responseObserverRef.get().onNext(lrp);

        io.etcd.jetcd.lease.LeaseKeepAliveResponse lrpActual = lrpFuture.get();
        assertThat(lrpActual.getID()).isEqualTo(lrp.getID());
    }

    @Test
    public void testKeepAliveOnceConnectError() throws ExecutionException, InterruptedException {
        CompletableFuture<io.etcd.jetcd.lease.LeaseKeepAliveResponse> lrpFuture = this.leaseCli.keepAliveOnce(LEASE_ID_1);
        Throwable t = Status.ABORTED.asRuntimeException();
        responseObserverRef.get().onError(t);

        assertThatThrownBy(lrpFuture::get).hasCause(toEtcdException(t));
    }

    @Test
    public void testKeepAliveOnceStreamCloseOnSuccess() throws ExecutionException, InterruptedException {
        CompletableFuture<io.etcd.jetcd.lease.LeaseKeepAliveResponse> lrpFuture = this.leaseCli.keepAliveOnce(LEASE_ID_1);
        LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse.newBuilder().setID(LEASE_ID_1).build();

        this.responseObserverRef.get().onNext(lrp);

        lrpFuture.get();

        verify(this.requestStreamObserverMock, timeout(100).times(1)).onCompleted();
    }

    /*
    // TODO: sometime this.responseObserverRef.get().onNext(lrp) blocks even though client has received msg;
    // seems like a bug in grpc test framework.
    @Ignore
    @Test
    public void testKeepAliveListenOnOneResponse() throws InterruptedException {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID)
        .setTTL(2)
        .build();
    System.out.println(System.currentTimeMillis() + " responseObserverRef.onNext() ");
    this.responseObserverRef.get().onNext(lrp);
    
    io.etcd.jetcd.lease.LeaseKeepAliveResponse actual = listener.listen();
    assertThat(actual.getID()).isEqualTo(lrp.getID());
    assertThat(actual.getTTL()).isEqualTo(lrp.getTTL());
    
    listener.close();
    }
    */

    @Test
    public void testKeepAliveOnSendingKeepAliveRequests() {
        final StreamObserver<io.etcd.jetcd.lease.LeaseKeepAliveResponse> observer = Observers.observer(response -> {
        });

        try (CloseableClient listener = this.leaseCli.keepAlive(LEASE_ID_1, observer)) {
            // expect more than one KeepAlive requests are sent within 1100 ms.
            verify(this.requestStreamObserverMock, timeout(1100).atLeast(2)).onNext(argThat(hasLeaseID(LEASE_ID_1)));
        }
    }

    @Test
    public void testKeepAliveAfterFirstKeepAliveTimeout() throws InterruptedException {
        final StreamObserver<io.etcd.jetcd.lease.LeaseKeepAliveResponse> observer = Observers.observer(response -> {
        });

        try (CloseableClient listener = this.leaseCli.keepAlive(LEASE_ID_1, observer)) {
            // expect at least some KeepAlive requests are sent within
            // firstKeepAliveTimeout(5000 ms) + some quiet time (1000 ms).
            verify(this.requestStreamObserverMock, after(6000).atLeastOnce()).onNext(argThat(hasLeaseID(LEASE_ID_1)));

            // reset mock to a empty state.
            Mockito.<StreamObserver> reset(this.requestStreamObserverMock);

            // verify no keepAlive requests are sent within one second after firstKeepAliveTimeout.
            verify(this.requestStreamObserverMock, after(1000).times(0)).onNext(argThat(hasLeaseID(LEASE_ID_1)));
        }
    }

    @Test
    @SuppressWarnings("FutureReturnValueIgnored") // ignore the future returned by timeToLive
    public void testTimeToLiveNullOption() {
        assertThatThrownBy(() -> this.leaseCli.timeToLive(LEASE_ID_1, null)).isInstanceOf(NullPointerException.class)
            .hasMessage("LeaseOption should not be null");
    }

    @Test
    public void testKeepAliveCloseOnlyListener() {
        final StreamObserver<io.etcd.jetcd.lease.LeaseKeepAliveResponse> observer = Observers.observer(response -> {
        });
        final CloseableClient client = this.leaseCli.keepAlive(LEASE_ID_1, observer);

        client.close();

        // expect no more keep alive requests are sent after the initial one
        // within 1 second after closing the only listener.
        verify(this.requestStreamObserverMock, after(1000).atMost(1)).onNext(argThat(hasLeaseID(LEASE_ID_1)));
    }

    @Test
    public void testKeepAliveCloseSomeListeners() {
        final StreamObserver<io.etcd.jetcd.lease.LeaseKeepAliveResponse> observer = Observers.observer(response -> {
        });
        final CloseableClient client1 = this.leaseCli.keepAlive(LEASE_ID_2, observer);
        final CloseableClient client2 = this.leaseCli.keepAlive(LEASE_ID_1, observer);

        client1.close();
        // expect closing closingListener doesn't affect sending keep alive requests for LEASE_ID_2.
        verify(this.requestStreamObserverMock, after(1200).atLeast(2)).onNext(argThat(hasLeaseID(LEASE_ID_1)));

        client1.close();
    }

    /*
    // TODO: sometime this.responseObserverRef.get().onNext(lrp) blocks even though client has received msg;
    // seems like a bug in grpc test framework.
    @Ignore
    @Test
    public void testKeepAliveReceivesExpiredLease() {
    KeepAliveListener listener = this.leaseCli.keepAlive(LEASE_ID_1);
    LeaseKeepAliveResponse lrp = LeaseKeepAliveResponse
        .newBuilder()
        .setID(LEASE_ID_1)
        .setTTL(0)
        .build();
    this.responseObserverRef.get().onNext(lrp);
    
    // expect lease expired exception.
    assertThatThrownBy(() -> listener.listen())
        .hasCause(new IllegalStateException("Lease " + LEASE_ID_1 + " expired"));
    
    // expect no more keep alive requests for LEASE_ID after receiving lease expired response.
    verify(this.requestStreamObserverMock, after(1000).atMost(1))
        .onNext(argThat(hasLeaseID(LEASE_ID_1)));
    
    listener.close();
    }
    */

    @Test
    public void testKeepAliveResetOnStreamErrors() {
        final StreamObserver<io.etcd.jetcd.lease.LeaseKeepAliveResponse> observer = Observers.observer(response -> {
        });

        try (CloseableClient client = this.leaseCli.keepAlive(LEASE_ID_1, observer)) {
            Throwable t = Status.ABORTED.asRuntimeException();
            // onError triggers reset() to be executed.
            // scheduler will execute reset() in 500 ms.
            responseObserverRef.get().onError(t);

            // expect keep alive requests are still sending even with reset.
            verify(this.requestStreamObserverMock, timeout(2000).atLeast(3)).onNext(argThat(hasLeaseID(LEASE_ID_1)));
        }
    }

    // return a ArgumentMatcher that checks if the captured LeaseKeepAliveRequest has same leaseId.
    private static ArgumentMatcher<LeaseKeepAliveRequest> hasLeaseID(long leaseId) {
        return o -> o.getID() == leaseId;
    }

    private static LeaseImplBase createLeaseImplBase(
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
