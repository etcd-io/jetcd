package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.api.ClusterGrpc;
import com.coreos.jetcd.api.MemberAddRequest;
import com.coreos.jetcd.api.MemberListRequest;
import com.coreos.jetcd.api.MemberRemoveRequest;
import com.coreos.jetcd.api.MemberUpdateRequest;
import com.coreos.jetcd.cluster.MemberAddResponse;
import com.coreos.jetcd.cluster.MemberListResponse;
import com.coreos.jetcd.cluster.MemberRemoveResponse;
import com.coreos.jetcd.cluster.MemberUpdateResponse;
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
    return Util.listenableToCompletableFuture(
        this.stub.memberList(MemberListRequest.getDefaultInstance()),
        Util::toMemberListResponse,
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
    return Util.listenableToCompletableFuture(
        this.stub.memberAdd(memberAddRequest),
        Util::toMemberAddResponse,
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
    return Util.listenableToCompletableFuture(
        this.stub.memberRemove(memberRemoveRequest),
        Util::toMemberRemoveResponse,
        this.connectionManager.getExecutorService()
    );
  }

  /**
   * update peer addresses of the member.
   *
   * @param memberID the id of member to update
   * @param endpoints the new endpoints for the member
   */
  @Override
  public CompletableFuture<MemberUpdateResponse> updateMember(
      long memberID, List<String> peerAddrs) {
    MemberUpdateRequest memberUpdateRequest = MemberUpdateRequest.newBuilder()
        .addAllPeerURLs(peerAddrs)
        .setID(memberID)
        .build();
    return Util.listenableToCompletableFuture(
        this.stub.memberUpdate(memberUpdateRequest),
        Util::toMemberUpdateResponse,
        this.connectionManager.getExecutorService()
    );
  }
}
