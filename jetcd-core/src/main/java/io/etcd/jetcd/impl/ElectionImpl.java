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

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Election;
import io.etcd.jetcd.api.CampaignRequest;
import io.etcd.jetcd.api.LeaderRequest;
import io.etcd.jetcd.api.ProclaimRequest;
import io.etcd.jetcd.api.ResignRequest;
import io.etcd.jetcd.api.VertxElectionGrpc;
import io.etcd.jetcd.election.CampaignResponse;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.election.NoLeaderException;
import io.etcd.jetcd.election.NotLeaderException;
import io.etcd.jetcd.election.ProclaimResponse;
import io.etcd.jetcd.election.ResignResponse;
import io.etcd.jetcd.support.Util;
import io.grpc.StatusRuntimeException;

import com.google.protobuf.ByteString;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

final class ElectionImpl extends Impl implements Election {
    private final VertxElectionGrpc.ElectionVertxStub stub;
    private final ByteSequence namespace;

    ElectionImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager.newStub(VertxElectionGrpc::newVertxStub);
        this.namespace = connectionManager.getNamespace();
    }

    @Override
    public CompletableFuture<CampaignResponse> campaign(ByteSequence electionName, long leaseId, ByteSequence proposal) {
        checkNotNull(electionName, "election name should not be null");
        checkNotNull(proposal, "proposal should not be null");

        CampaignRequest request = CampaignRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName, namespace))
            .setValue(ByteString.copyFrom(proposal.getBytes()))
            .setLease(leaseId)
            .build();

        return completable(
            stub.campaign(request),
            CampaignResponse::new,
            this::convertException);
    }

    @Override
    public CompletableFuture<ProclaimResponse> proclaim(LeaderKey leaderKey, ByteSequence proposal) {
        checkNotNull(leaderKey, "leader key should not be null");
        checkNotNull(proposal, "proposal should not be null");

        ProclaimRequest request = ProclaimRequest.newBuilder()
            .setLeader(
                io.etcd.jetcd.api.LeaderKey.newBuilder()
                    .setKey(ByteString.copyFrom(leaderKey.getKey().getBytes()))
                    .setName(ByteString.copyFrom(leaderKey.getName().getBytes()))
                    .setLease(leaderKey.getLease())
                    .setRev(leaderKey.getRevision())
                    .build())
            .setValue(ByteString.copyFrom(proposal.getBytes()))
            .build();

        return completable(
            stub.proclaim(request),
            ProclaimResponse::new,
            this::convertException);
    }

    @Override
    public CompletableFuture<LeaderResponse> leader(ByteSequence electionName) {
        checkNotNull(electionName, "election name should not be null");

        LeaderRequest request = LeaderRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName, namespace))
            .build();

        return completable(
            stub.leader(request),
            r -> new LeaderResponse(r, namespace),
            this::convertException);
    }

    @Override
    public void observe(ByteSequence electionName, Listener listener) {
        checkNotNull(electionName, "election name should not be null");
        checkNotNull(listener, "listener should not be null");

        LeaderRequest request = LeaderRequest.newBuilder()
            .setName(ByteString.copyFrom(electionName.getBytes()))
            .build();

        stub.observe(request)
            .handler(value -> listener.onNext(new LeaderResponse(value, namespace)))
            .endHandler(ignored -> listener.onCompleted())
            .exceptionHandler(error -> listener.onError(toEtcdException(error)));
    }

    @Override
    public CompletableFuture<ResignResponse> resign(LeaderKey leaderKey) {
        checkNotNull(leaderKey, "leader key should not be null");

        ResignRequest request = ResignRequest.newBuilder()
            .setLeader(
                io.etcd.jetcd.api.LeaderKey.newBuilder()
                    .setKey(ByteString.copyFrom(leaderKey.getKey().getBytes()))
                    .setName(ByteString.copyFrom(leaderKey.getName().getBytes()))
                    .setLease(leaderKey.getLease())
                    .setRev(leaderKey.getRevision())
                    .build())
            .build();

        return completable(
            stub.resign(request),
            ResignResponse::new,
            this::convertException);
    }

    private Throwable convertException(Throwable e) {
        if (e instanceof StatusRuntimeException) {
            StatusRuntimeException exception = (StatusRuntimeException) e;
            String description = exception.getStatus().getDescription();
            // different APIs use different messages. we cannot distinguish missing leader error otherwise,
            // because communicated status is always UNKNOWN
            if ("election: not leader".equals(description)) {
                return new NotLeaderException();
            } else if ("election: no leader".equals(description)) {
                return new NoLeaderException();
            }
        }

        return toEtcdException(e);
    }
}
