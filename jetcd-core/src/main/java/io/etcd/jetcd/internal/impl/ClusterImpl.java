/**
 * Copyright 2017 The jetcd authors
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

package io.etcd.jetcd.internal.impl;

import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.api.ClusterGrpc;
import io.etcd.jetcd.api.MemberAddRequest;
import io.etcd.jetcd.api.MemberListRequest;
import io.etcd.jetcd.api.MemberRemoveRequest;
import io.etcd.jetcd.api.MemberUpdateRequest;
import io.etcd.jetcd.cluster.MemberAddResponse;
import io.etcd.jetcd.cluster.MemberListResponse;
import io.etcd.jetcd.cluster.MemberRemoveResponse;
import io.etcd.jetcd.cluster.MemberUpdateResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of cluster client.
 */
class ClusterImpl implements Cluster {

  private final ClusterGrpc.ClusterFutureStub stub;
  private final ClientConnectionManager connectionManager;

  ClusterImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(ClusterGrpc::newFutureStub);
  }

  /**
   * lists the current cluster membership.
   */
  @Override
  public CompletableFuture<MemberListResponse> listMember() {
    return Util.toCompletableFuture(
        this.stub.memberList(MemberListRequest.getDefaultInstance()),
        MemberListResponse::new,
        this.connectionManager.getExecutorService()
    );
  }

  /**
   * add a new member into the cluster.
   *
   * @param peerAddrs the peer addresses of the new member
   */
  @Override
  public CompletableFuture<MemberAddResponse> addMember(List<String> peerAddrs) {
    MemberAddRequest memberAddRequest = MemberAddRequest.newBuilder()
        .addAllPeerURLs(peerAddrs)
        .build();
    return Util.toCompletableFuture(
        this.stub.memberAdd(memberAddRequest),
        MemberAddResponse::new,
        this.connectionManager.getExecutorService()
    );
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
    return Util.toCompletableFuture(
        this.stub.memberRemove(memberRemoveRequest),
        MemberRemoveResponse::new,
        this.connectionManager.getExecutorService()
    );
  }

  /**
   * update peer addresses of the member.
   *
   * @param memberID the id of member to update
   * @param peerAddrs the new endpoints for the member
   */
  @Override
  public CompletableFuture<MemberUpdateResponse> updateMember(
      long memberID, List<String> peerAddrs) {
    MemberUpdateRequest memberUpdateRequest = MemberUpdateRequest.newBuilder()
        .addAllPeerURLs(peerAddrs)
        .setID(memberID)
        .build();
    return Util.toCompletableFuture(
        this.stub.memberUpdate(memberUpdateRequest),
        MemberUpdateResponse::new,
        this.connectionManager.getExecutorService()
    );
  }
}
