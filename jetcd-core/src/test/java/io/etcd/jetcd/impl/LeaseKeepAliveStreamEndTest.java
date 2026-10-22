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

package io.etcd.jetcd.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.api.LeaseGrpc.LeaseImplBase;
import io.etcd.jetcd.api.LeaseKeepAliveRequest;
import io.etcd.jetcd.api.LeaseKeepAliveResponse;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.support.Observers;
import io.etcd.jetcd.test.GrpcServerExtension;
import io.grpc.stub.StreamObserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Keep-alive behaviour when the server ends a {@code LeaseKeepAlive} stream with status OK rather than an
 * error, as an etcd member does when it shuts down gracefully or a proxy in front of it closes the stream.
 */
@Timeout(value = 1, unit = TimeUnit.MINUTES)
public class LeaseKeepAliveStreamEndTest {
    private static final long TTL_SECONDS = 3;
    private static final long FIRST_LEASE_ID = 1;
    private static final long SECOND_LEASE_ID = 2;

    @RegisterExtension
    public final GrpcServerExtension grpcServer = new GrpcServerExtension().directExecutor();

    private StreamEndingLease leaseService;
    private Lease leaseClient;

    @BeforeEach
    public void setUp() {
        leaseService = new StreamEndingLease();
        grpcServer.getServiceRegistry().addService(leaseService);
        leaseClient = new LeaseImpl(new ClientConnectionManager(Client.builder(), grpcServer.getChannel()));
    }

    @AfterEach
    public void tearDown() {
        leaseClient.close();
    }

    @Test
    public void keepAliveContinuesAfterTheServerEndsTheStream() {
        leaseService.endFirstStreamAfterFirstResponse();
        AtomicInteger renewals = new AtomicInteger();

        leaseClient.keepAlive(FIRST_LEASE_ID, Observers.observer(response -> renewals.incrementAndGet()));

        await().atMost(TTL_SECONDS * 3, TimeUnit.SECONDS).untilAsserted(() -> assertThat(renewals).hasValueGreaterThan(1));
    }

    @Test
    public void keepAliveRenewsALeaseAddedAfterTheServerEndsTheStream() {
        leaseService.endFirstStreamAfterFirstResponse();
        leaseClient.keepAlive(FIRST_LEASE_ID, Observers.observer(response -> {
        }));
        await().atMost(TTL_SECONDS, TimeUnit.SECONDS).until(leaseService::firstStreamEnded);
        AtomicInteger renewals = new AtomicInteger();

        leaseClient.keepAlive(SECOND_LEASE_ID, Observers.observer(response -> renewals.incrementAndGet()));

        await().atMost(TTL_SECONDS * 3, TimeUnit.SECONDS).untilAsserted(() -> assertThat(renewals).hasValueGreaterThan(0));
    }

    @Test
    public void keepAliveReopensTheStreamOnceAfterTheServerEndsIt() {
        leaseService.endFirstStreamAfterFirstResponse();

        leaseClient.keepAlive(FIRST_LEASE_ID, Observers.observer(response -> {
        }));

        await().atMost(TTL_SECONDS * 3, TimeUnit.SECONDS)
            .during(TTL_SECONDS, TimeUnit.SECONDS)
            .until(leaseService::streamsOpened, streams -> streams == 2);
    }

    @Test
    public void keepAliveOnceFailsWhenTheServerEndsTheStreamWithoutAResponse() {
        leaseService.endFirstStreamWithoutResponding();

        CompletableFuture<io.etcd.jetcd.lease.LeaseKeepAliveResponse> renewal = leaseClient.keepAliveOnce(FIRST_LEASE_ID);

        assertThatThrownBy(() -> renewal.get(TTL_SECONDS, TimeUnit.SECONDS)).hasCauseInstanceOf(EtcdException.class);
    }

    /**
     * A lease service that answers every keep-alive request with {@link #TTL_SECONDS}, and can end the first
     * {@code LeaseKeepAlive} stream it is opened with status OK.
     */
    private static final class StreamEndingLease extends LeaseImplBase {
        private final AtomicInteger streamsOpened = new AtomicInteger();
        private volatile boolean endFirstStream;
        private volatile boolean respondBeforeEnding;
        private volatile boolean firstStreamEnded;

        void endFirstStreamAfterFirstResponse() {
            endFirstStream = true;
            respondBeforeEnding = true;
        }

        void endFirstStreamWithoutResponding() {
            endFirstStream = true;
            respondBeforeEnding = false;
        }

        boolean firstStreamEnded() {
            return firstStreamEnded;
        }

        int streamsOpened() {
            return streamsOpened.get();
        }

        @Override
        public StreamObserver<LeaseKeepAliveRequest> leaseKeepAlive(StreamObserver<LeaseKeepAliveResponse> responses) {
            boolean endThisStream = streamsOpened.incrementAndGet() == 1 && endFirstStream;

            return new StreamObserver<LeaseKeepAliveRequest>() {
                private boolean ended;

                @Override
                public synchronized void onNext(LeaseKeepAliveRequest request) {
                    if (ended) {
                        return;
                    }
                    if (!endThisStream || respondBeforeEnding) {
                        responses
                            .onNext(LeaseKeepAliveResponse.newBuilder().setID(request.getID()).setTTL(TTL_SECONDS).build());
                    }
                    if (endThisStream) {
                        end();
                        firstStreamEnded = true;
                    }
                }

                @Override
                public synchronized void onError(Throwable throwable) {
                    ended = true;
                }

                @Override
                public synchronized void onCompleted() {
                    end();
                }

                private void end() {
                    if (!ended) {
                        ended = true;
                        responses.onCompleted();
                    }
                }
            };
        }
    }
}
