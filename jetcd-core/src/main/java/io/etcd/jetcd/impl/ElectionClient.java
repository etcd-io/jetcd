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
import io.etcd.jetcd.election.CampaignResponse;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.election.NoLeaderException;
import io.etcd.jetcd.election.NotLeaderException;
import io.etcd.jetcd.election.ProclaimResponse;
import io.etcd.jetcd.election.ResignResponse;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.support.Util;
import io.vertx.grpc.client.GrpcClientRequestProvider;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcStatus;

import com.google.protobuf.ByteString;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static java.util.Objects.requireNonNull;

final class ElectionClient extends AbstractClient implements Election {
    private static final String NO_LEADER = "election: no leader";
    private static final String NOT_LEADER = "election: not leader";

    private final GrpcClientRequestProvider requests;
    private final ByteSequence namespace;

    ElectionClient(GrpcService grpcService) {
        super(grpcService);

        this.requests = grpcService.getAuthenticatedGrpcClient().bind(grpcService.getServiceResolver().getTarget());
        this.namespace = grpcService.getNamespace();
    }

    @Override
    public CompletableFuture<CampaignResponse> campaign(ByteSequence electionName, long leaseId, ByteSequence proposal) {
        requireNonNull(electionName, "election name should not be null");
        requireNonNull(proposal, "proposal should not be null");

        io.etcd.jetcd.api.CampaignRequest request = io.etcd.jetcd.api.CampaignRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName, namespace))
            .setValue(ByteString.copyFrom(proposal.getBytes()))
            .setLease(leaseId)
            .build();

        return executeCancellable(
            () -> grpc().auth().request(io.etcd.jetcd.api.ElectionGrpcClient.Campaign),
            request,
            responseFactory::newCampaignResponse,
            false,
            ElectionClient::convertException);
    }

    @Override
    public CompletableFuture<ProclaimResponse> proclaim(LeaderKey leaderKey, ByteSequence proposal) {
        requireNonNull(leaderKey, "leader key should not be null");
        requireNonNull(proposal, "proposal should not be null");

        io.etcd.jetcd.api.ProclaimRequest request = io.etcd.jetcd.api.ProclaimRequest.newBuilder()
            .setLeader(
                io.etcd.jetcd.api.LeaderKey.newBuilder()
                    .setKey(ByteString.copyFrom(leaderKey.key().getBytes()))
                    .setName(ByteString.copyFrom(leaderKey.name().getBytes()))
                    .setLease(leaderKey.lease())
                    .setRev(leaderKey.revision())
                    .build())
            .setValue(ByteString.copyFrom(proposal.getBytes()))
            .build();

        return execute(
            io.etcd.jetcd.api.ElectionGrpcClient.Proclaim,
            request,
            responseFactory::newProclaimResponse,
            false,
            ElectionClient::convertException);
    }

    @Override
    public CompletableFuture<LeaderResponse> leader(ByteSequence electionName) {
        requireNonNull(electionName, "election name should not be null");

        io.etcd.jetcd.api.LeaderRequest request = io.etcd.jetcd.api.LeaderRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName, namespace))
            .build();

        return execute(
            io.etcd.jetcd.api.ElectionGrpcClient.Leader,
            request,
            responseFactory::newLeaderResponse,
            false,
            ElectionClient::convertException);
    }

    @Override
    public void observe(ByteSequence electionName, Listener listener) {
        requireNonNull(electionName, "election name should not be null");
        requireNonNull(listener, "listener should not be null");

        io.etcd.jetcd.api.LeaderRequest request = io.etcd.jetcd.api.LeaderRequest.newBuilder()
            .setName(Util.prefixNamespace(electionName, namespace))
            .build();

        var stream = ServerStreamingCall.consume(requests, io.etcd.jetcd.api.ElectionGrpcClient.Observe, request,
            value -> listener.onNext(responseFactory.newLeaderResponse(value)));
        ServerStreamingCall.notifyListener(grpc().vertx(), stream, listener::onError, listener::onCompleted);
    }

    @Override
    public CompletableFuture<ResignResponse> resign(LeaderKey leaderKey) {
        requireNonNull(leaderKey, "leader key should not be null");

        io.etcd.jetcd.api.ResignRequest request = io.etcd.jetcd.api.ResignRequest.newBuilder()
            .setLeader(
                io.etcd.jetcd.api.LeaderKey.newBuilder()
                    .setKey(ByteString.copyFrom(leaderKey.key().getBytes()))
                    .setName(ByteString.copyFrom(leaderKey.name().getBytes()))
                    .setLease(leaderKey.lease())
                    .setRev(leaderKey.revision())
                    .build())
            .build();

        return execute(
            io.etcd.jetcd.api.ElectionGrpcClient.Resign,
            request,
            responseFactory::newResignResponse,
            false,
            ElectionClient::convertException);
    }

    private static RuntimeException convertException(Throwable error) {
        if (error instanceof GrpcResponseException response) {
            String message = response.getMessage();
            if (response.status() == GrpcStatus.UNKNOWN) {
                if (NO_LEADER.equals(message)) {
                    return new NoLeaderException();
                }
                if (NOT_LEADER.equals(message)) {
                    return new NotLeaderException();
                }
            }
            return toEtcdException(response.status(), message, response.getCause());
        }
        if (error instanceof InvalidStatusException status) {
            return toEtcdException(status.actualStatus(), null, status);
        }
        return toEtcdException(error);
    }
}
