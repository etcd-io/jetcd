package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.api.ClusterGrpc;
import com.coreos.jetcd.api.MemberAddRequest;
import com.coreos.jetcd.api.MemberAddResponse;
import com.coreos.jetcd.api.MemberListRequest;
import com.coreos.jetcd.api.MemberListResponse;
import com.coreos.jetcd.api.MemberRemoveRequest;
import com.coreos.jetcd.api.MemberRemoveResponse;
import com.coreos.jetcd.api.MemberUpdateRequest;
import com.coreos.jetcd.api.MemberUpdateResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

/**
 * Implementation of cluster client.
 */
class ClusterImpl implements Cluster {
  private final ClusterGrpc.ClusterFutureStub stub;

  ClusterImpl(ClientConnectionManager connectionManager) {
    this.stub = connectionManager.newStub(ClusterGrpc::newFutureStub);
  }

  /**
   * lists the current cluster membership.
   */
  @Override
  public CompletableFuture<MemberListResponse> listMember() {
    return FutureConverter.toCompletableFuture(
        this.stub.memberList(MemberListRequest.getDefaultInstance())
    );
  }

  /**
   * add a new member into the cluster.
   *
   * @param endpoints the address of the new member
   */
  @Override
  public CompletableFuture<MemberAddResponse> addMember(List<String> endpoints) {
    MemberAddRequest memberAddRequest = MemberAddRequest.newBuilder()
        .addAllPeerURLs(endpoints)
        .build();
    return FutureConverter.toCompletableFuture(this.stub.memberAdd(memberAddRequest));
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
    return FutureConverter.toCompletableFuture(this.stub.memberRemove(memberRemoveRequest));
  }

  /**
   * update peer addresses of the member.
   *
   * @param memberID the id of member to update
   * @param endpoints the new endpoints for the member
   */
  @Override
  public CompletableFuture<MemberUpdateResponse> updateMember(
      long memberID, List<String> endpoints) {
    MemberUpdateRequest memberUpdateRequest = MemberUpdateRequest.newBuilder()
        .addAllPeerURLs(endpoints)
        .setID(memberID)
        .build();
    return FutureConverter.toCompletableFuture(this.stub.memberUpdate(memberUpdateRequest));
  }
}
