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
import io.etcd.jetcd.api.MemberAddRequest;
import io.etcd.jetcd.api.MemberListRequest;
import io.etcd.jetcd.api.MemberRemoveRequest;
import io.etcd.jetcd.api.MemberUpdateRequest;
import io.etcd.jetcd.api.VertxClusterGrpc;
import io.etcd.jetcd.cluster.MemberAddResponse;
import io.etcd.jetcd.cluster.MemberListResponse;
import io.etcd.jetcd.cluster.MemberRemoveResponse;
import io.etcd.jetcd.cluster.MemberUpdateResponse;

/**
 * Implementation of cluster client.
 */
final class ClusterImpl extends Impl implements Cluster {

    private final VertxClusterGrpc.ClusterVertxStub stub;

    ClusterImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager.newStub(VertxClusterGrpc::newVertxStub);
    }

    /**
     * lists the current cluster membership.
     */
    @Override
    public CompletableFuture<MemberListResponse> listMember() {
        return completable(
            this.stub.memberList(MemberListRequest.getDefaultInstance()),
            MemberListResponse::new);
    }

    /**
     * add a new member into the cluster.
     *
     * @param peerAddrs the peer addresses of the new member
     * @param isLearner whether the member is raft learner
     */
    @Override
    public CompletableFuture<MemberAddResponse> addMember(List<URI> peerAddrs, boolean isLearner) {
        MemberAddRequest memberAddRequest = MemberAddRequest.newBuilder()
            .addAllPeerURLs(peerAddrs.stream().map(URI::toString).collect(Collectors.toList()))
            .setIsLearner(isLearner)
            .build();

        return completable(
            this.stub.memberAdd(memberAddRequest),
            MemberAddResponse::new);
    }

    /**
     * removes an existing member from the cluster.
     *
     * @param memberID the id of the member
     */
    @Override
    public CompletableFuture<MemberRemoveResponse> removeMember(long memberID) {
        MemberRemoveRequest memberRemoveRequest = MemberRemoveRequest.newBuilder()
            .setID(memberID)
            .build();

        return completable(
            this.stub.memberRemove(memberRemoveRequest),
            MemberRemoveResponse::new);
    }

    /**
     * update peer addresses of the member.
     *
     * @param memberID  the id of member to update
     * @param peerAddrs the new endpoints for the member
     */
    @Override
    public CompletableFuture<MemberUpdateResponse> updateMember(long memberID, List<URI> peerAddrs) {
        MemberUpdateRequest memberUpdateRequest = MemberUpdateRequest.newBuilder()
            .addAllPeerURLs(peerAddrs.stream().map(URI::toString).collect(Collectors.toList()))
            .setID(memberID)
            .build();

        return completable(
            this.stub.memberUpdate(memberUpdateRequest),
            MemberUpdateResponse::new);
    }
}
