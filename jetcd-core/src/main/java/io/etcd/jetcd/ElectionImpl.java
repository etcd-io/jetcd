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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.etcd.jetcd.api.CampaignRequest;
import io.etcd.jetcd.api.ElectionGrpc;
import io.etcd.jetcd.api.LeaderRequest;
import io.etcd.jetcd.api.ProclaimRequest;
import io.etcd.jetcd.api.ResignRequest;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.election.CampaignResponse;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.election.NoLeaderException;
import io.etcd.jetcd.election.NotLeaderException;
import io.etcd.jetcd.election.ProclaimResponse;
import io.etcd.jetcd.election.ResignResponse;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import static com.google.common.base.Preconditions.checkNotNull;

final class ElectionImpl implements Election {
    private final ElectionGrpc.ElectionStub stub;
    private final ByteSequence namespace;

    ElectionImpl(ClientConnectionManager connectionManager) {
        this.stub = connectionManager.newStub(ElectionGrpc::newStub);
        this.namespace = connectionManager.getNamespace();
    }

    @Override
    public CompletableFuture<CampaignResponse> campaign(ByteSequence electionName, long leaseId, ByteSequence proposal) {
        checkNotNull(electionName, "election name should not be null");
        checkNotNull(proposal, "proposal should not be null");

        CampaignRequest request = CampaignRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName.getByteString(), namespace))
            .setValue(proposal.getByteString())
            .setLease(leaseId)
            .build();

        final StreamObserverDelegate<io.etcd.jetcd.api.CampaignResponse, CampaignResponse> observer = new StreamObserverDelegate<>(
            CampaignResponse::new);
        stub.campaign(request, observer);

        return observer.getFuture();
    }

    @Override
    public CompletableFuture<ProclaimResponse> proclaim(LeaderKey leaderKey, ByteSequence proposal) {
        checkNotNull(leaderKey, "leader key should not be null");
        checkNotNull(proposal, "proposal should not be null");

        io.etcd.jetcd.api.LeaderKey leader = io.etcd.jetcd.api.LeaderKey.newBuilder()
            .setKey(leaderKey.getKey()).setName(leaderKey.getName())
            .setLease(leaderKey.getLease()).setRev(leaderKey.getRevision())
            .build();
        ProclaimRequest request = ProclaimRequest.newBuilder()
            .setLeader(leader).setValue(proposal.getByteString())
            .build();

        final StreamObserverDelegate<io.etcd.jetcd.api.ProclaimResponse, ProclaimResponse> observer = new StreamObserverDelegate<>(
            ProclaimResponse::new);
        stub.proclaim(request, observer);

        return observer.getFuture();
    }

    @Override
    public CompletableFuture<LeaderResponse> leader(ByteSequence electionName) {
        checkNotNull(electionName, "election name should not be null");

        LeaderRequest request = LeaderRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName.getByteString(), namespace))
            .build();

        final StreamObserverDelegate<io.etcd.jetcd.api.LeaderResponse, LeaderResponse> observer = new StreamObserverDelegate<>(
            input -> new LeaderResponse(input, namespace));
        stub.leader(request, observer);

        return observer.getFuture();
    }

    @Override
    public void observe(ByteSequence electionName, Listener listener) {
        checkNotNull(electionName, "election name should not be null");
        checkNotNull(listener, "listener should not be null");

        LeaderRequest request = LeaderRequest.newBuilder().setName(electionName.getByteString()).build();

        stub.observe(request, new StreamObserver<io.etcd.jetcd.api.LeaderResponse>() {
            @Override
            public void onNext(io.etcd.jetcd.api.LeaderResponse value) {
                listener.onNext(new LeaderResponse(value, namespace));
            }

            @Override
            public void onError(Throwable error) {
                listener.onError(EtcdExceptionFactory.toEtcdException(error));
            }

            @Override
            public void onCompleted() {
                listener.onCompleted();
            }
        });
    }

    @Override
    public CompletableFuture<ResignResponse> resign(LeaderKey leaderKey) {
        checkNotNull(leaderKey, "leader key should not be null");

        io.etcd.jetcd.api.LeaderKey leader = io.etcd.jetcd.api.LeaderKey.newBuilder()
            .setKey(leaderKey.getKey()).setName(leaderKey.getName())
            .setLease(leaderKey.getLease()).setRev(leaderKey.getRevision())
            .build();
        ResignRequest request = ResignRequest.newBuilder().setLeader(leader).build();

        final StreamObserverDelegate<io.etcd.jetcd.api.ResignResponse, ResignResponse> observer = new StreamObserverDelegate<>(
            ResignResponse::new);
        stub.resign(request, observer);

        return observer.getFuture();
    }

    @Override
    public void close() {
    }

    private static class StreamObserverDelegate<S, T> implements StreamObserver<S> {
        private final CompletableFuture<T> future = new CompletableFuture<>();
        private final Function<S, T> converter;

        public StreamObserverDelegate(Function<S, T> converter) {
            this.converter = converter;
        }

        @Override
        public void onNext(S value) {
            future.complete(converter.apply(value));
        }

        @Override
        public void onError(Throwable error) {
            if (error instanceof StatusRuntimeException) {
                StatusRuntimeException exception = (StatusRuntimeException) error;
                String description = exception.getStatus().getDescription();
                // different APIs use different messages. we cannot distinguish missing leader error otherwise,
                // because communicated status is always UNKNOWN
                if ("election: not leader".equals(description)) {
                    future.completeExceptionally(NotLeaderException.INSTANCE);
                } else if ("election: no leader".equals(description)) {
                    future.completeExceptionally(NoLeaderException.INSTANCE);
                }
            }
            future.completeExceptionally(EtcdExceptionFactory.toEtcdException(error));
        }

        @Override
        public void onCompleted() {
        }

        public CompletableFuture<T> getFuture() {
            return future;
        }
    }
}
