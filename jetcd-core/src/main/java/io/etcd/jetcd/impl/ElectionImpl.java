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
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.support.Util;
import io.grpc.StatusRuntimeException;

import com.google.protobuf.ByteString;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static java.util.Objects.requireNonNull;

final class ElectionImpl extends Impl implements Election {
    private final VertxElectionGrpc.ElectionVertxStub stub;
    private final ByteSequence namespace;

    ElectionImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager.newStub(VertxElectionGrpc::newVertxStub);
        this.namespace = connectionManager.getNamespace();
    }

    // Election operations are done in a context where a client is trying to implement
    // some fault tolerance related use case; in that type of context, it makes sense to always
    // apply require leader, since we don't want a client connected to a non-raft-leader server
    // have an election method just go silent if the server the client happens to be connected to
    // becomes partitioned from the actual raft-leader server in the etcd servers cluster:
    // in that scenario and without required leader, an attempt to campaign could block forever
    // not because some other client is already an election leader, but because the server the client
    // is connected to is partitioned and can't tell.
    // With require leader, in that case the call will fail and we give
    // the client the ability to (a) know (b) retry on a different server.
    // The retry on a different server should happen automatically if the connection manager is using
    // a round robin strategy.
    //
    // Beware in the context of this election API, the word "leader" is overloaded.
    // In the paragraph above when we say "raft-leader" we are talking about the etcd server that is a leader
    // of the etcd servers cluster according to raft, we are not talking about the client that
    // happens to be the leader of an election using the election API in this file.
    private VertxElectionGrpc.ElectionVertxStub stubWithLeader() {
        return Util.applyRequireLeader(true, stub);
    }

    @Override
    public CompletableFuture<CampaignResponse> campaign(ByteSequence electionName, long leaseId, ByteSequence proposal) {
        requireNonNull(electionName, "election name should not be null");
        requireNonNull(proposal, "proposal should not be null");

        CampaignRequest request = CampaignRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName, namespace))
            .setValue(ByteString.copyFrom(proposal.getBytes()))
            .setLease(leaseId)
            .build();

        return wrapConvertException(
            execute(
                () -> stubWithLeader().campaign(request),
                CampaignResponse::new,
                Errors::isSafeRetryMutableRPC));
    }

    @Override
    public CompletableFuture<ProclaimResponse> proclaim(LeaderKey leaderKey, ByteSequence proposal) {
        requireNonNull(leaderKey, "leader key should not be null");
        requireNonNull(proposal, "proposal should not be null");

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

        return wrapConvertException(
            execute(
                () -> stubWithLeader().proclaim(request),
                ProclaimResponse::new,
                Errors::isSafeRetryMutableRPC));
    }

    @Override
    public CompletableFuture<LeaderResponse> leader(ByteSequence electionName) {
        requireNonNull(electionName, "election name should not be null");

        LeaderRequest request = LeaderRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName, namespace))
            .build();

        return wrapConvertException(
            execute(
                () -> stubWithLeader().leader(request),
                response -> new LeaderResponse(response, namespace),
                Errors::isSafeRetryImmutableRPC));
    }

    @Override
    public void observe(ByteSequence electionName, Listener listener) {
        requireNonNull(electionName, "election name should not be null");
        requireNonNull(listener, "listener should not be null");

        LeaderRequest request = LeaderRequest.newBuilder()
            .setName(ByteString.copyFrom(electionName.getBytes()))
            .build();

        stubWithLeader().observeWithHandler(request,
            value -> listener.onNext(new LeaderResponse(value, namespace)),
            ignored -> listener.onCompleted(),
            error -> listener.onError(toEtcdException(error)));
    }

    @Override
    public CompletableFuture<ResignResponse> resign(LeaderKey leaderKey) {
        requireNonNull(leaderKey, "leader key should not be null");

        ResignRequest request = ResignRequest.newBuilder()
            .setLeader(
                io.etcd.jetcd.api.LeaderKey.newBuilder()
                    .setKey(ByteString.copyFrom(leaderKey.getKey().getBytes()))
                    .setName(ByteString.copyFrom(leaderKey.getName().getBytes()))
                    .setLease(leaderKey.getLease())
                    .setRev(leaderKey.getRevision())
                    .build())
            .build();

        return wrapConvertException(
            execute(
                () -> stubWithLeader().resign(request),
                ResignResponse::new,
                Errors::isSafeRetryMutableRPC));
    }

    private <S> CompletableFuture<S> wrapConvertException(CompletableFuture<S> future) {
        return future.exceptionally(e -> {
            throw convertException(e);
        });
    }

    private RuntimeException convertException(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof StatusRuntimeException) {
                StatusRuntimeException exception = (StatusRuntimeException) cause;
                String description = exception.getStatus().getDescription();
                // different APIs use different messages. we cannot distinguish missing leader error otherwise,
                // because communicated status is always UNKNOWN
                if ("election: not leader".equals(description)) {
                    // Candidate is not a leader at the moment.
                    // Note there is a one letter difference, but this exception type is not the same as
                    // NoLeaderException.
                    return new NotLeaderException();
                }
                if ("election: no leader".equals(description)) {
                    // Leader for given election does not exist.
                    // Note there is a one letter difference, but this exception type is not the same as
                    // NotLeaderException.
                    return new NoLeaderException();
                }
            }
            cause = cause.getCause();
        }
        return toEtcdException(e);
    }
}
