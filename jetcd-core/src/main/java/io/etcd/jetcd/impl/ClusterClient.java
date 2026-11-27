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

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.cluster.MemberAddResponse;
import io.etcd.jetcd.cluster.MemberListResponse;
import io.etcd.jetcd.cluster.MemberPromoteResponse;
import io.etcd.jetcd.cluster.MemberRemoveResponse;
import io.etcd.jetcd.cluster.MemberUpdateResponse;
import io.etcd.jetcd.grpc.GrpcService;

/**
 * Implementation of cluster client.
 */
final class ClusterClient extends AbstractClient implements Cluster {

    private final io.etcd.jetcd.api.ClusterGrpcClient client;

    ClusterClient(GrpcService grpcService) {
        super(grpcService);

        this.client = io.etcd.jetcd.api.ClusterGrpcClient.create(
            grpcService.getAuthenticatedGrpcClient(),
            grpcService.getServiceResolver().getTarget());
    }

    @Override
    public CompletableFuture<MemberListResponse> listMember() {
        return completable(
            client.memberList(io.etcd.jetcd.api.MemberListRequest.getDefaultInstance()),
            MemberListResponse::new);
    }

    @Override
    public CompletableFuture<MemberAddResponse> addMember(List<URI> peerAddrs) {
        return addMember(peerAddrs, false);
    }

    @Override
    public CompletableFuture<MemberAddResponse> addMember(List<URI> peerAddrs, boolean isLearner) {
        io.etcd.jetcd.api.MemberAddRequest memberAddRequest = io.etcd.jetcd.api.MemberAddRequest.newBuilder()
            .addAllPeerURLs(peerAddrs.stream().map(URI::toString).collect(Collectors.toList()))
            .setIsLearner(isLearner)
            .build();

        return completable(
            client.memberAdd(memberAddRequest),
            MemberAddResponse::new);
    }

    @Override
    public CompletableFuture<MemberRemoveResponse> removeMember(long memberID) {
        io.etcd.jetcd.api.MemberRemoveRequest memberRemoveRequest = io.etcd.jetcd.api.MemberRemoveRequest.newBuilder()
            .setID(memberID)
            .build();

        return completable(
            client.memberRemove(memberRemoveRequest),
            MemberRemoveResponse::new);
    }

    @Override
    public CompletableFuture<MemberUpdateResponse> updateMember(long memberID, List<URI> peerAddrs) {
        io.etcd.jetcd.api.MemberUpdateRequest memberUpdateRequest = io.etcd.jetcd.api.MemberUpdateRequest.newBuilder()
            .addAllPeerURLs(peerAddrs.stream().map(URI::toString).collect(Collectors.toList()))
            .setID(memberID)
            .build();

        return completable(
            client.memberUpdate(memberUpdateRequest),
            MemberUpdateResponse::new);
    }

    @Override
    public CompletableFuture<MemberPromoteResponse> promoteMember(long memberID) {
        io.etcd.jetcd.api.MemberPromoteRequest memberPromoteRequest = io.etcd.jetcd.api.MemberPromoteRequest.newBuilder()
            .setID(memberID)
            .build();

        return completable(
            client.memberPromote(memberPromoteRequest),
            MemberPromoteResponse::new);
    }

}
