package com.coreos.jetcd;

import com.coreos.jetcd.api.ClusterGrpc;
import com.coreos.jetcd.api.MemberAddRequest;
import com.coreos.jetcd.api.MemberAddResponse;
import com.coreos.jetcd.api.MemberListRequest;
import com.coreos.jetcd.api.MemberListResponse;
import com.coreos.jetcd.api.MemberRemoveRequest;
import com.coreos.jetcd.api.MemberRemoveResponse;
import com.coreos.jetcd.api.MemberUpdateRequest;
import com.coreos.jetcd.api.MemberUpdateResponse;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of cluster client.
 */
public class EtcdClusterImpl implements EtcdCluster {

  private final ClusterGrpc.ClusterFutureStub stub;

  public EtcdClusterImpl(ManagedChannel channel, Optional<String> token) {
    this.stub = EtcdClientUtil.configureStub(ClusterGrpc.newFutureStub(channel), token);
  }

  /**
   * lists the current cluster membership.
   */
  @Override
  public ListenableFuture<MemberListResponse> listMember() {
    return stub.memberList(MemberListRequest.getDefaultInstance());
  }

  /**
   * add a new member into the cluster.
   *
   * @param endpoints the address of the new member
   */
  @Override
  public ListenableFuture<MemberAddResponse> addMember(List<String> endpoints) {
    MemberAddRequest memberAddRequest = MemberAddRequest.newBuilder().addAllPeerURLs(endpoints)
        .build();
    return stub.memberAdd(memberAddRequest);
  }

  /**
   * removes an existing member from the cluster.
   *
   * @param memberID the id of the member
   */
  @Override
  public ListenableFuture<MemberRemoveResponse> removeMember(long memberID) {
    MemberRemoveRequest memberRemoveRequest = MemberRemoveRequest.newBuilder().setID(memberID)
        .build();
    return stub.memberRemove(memberRemoveRequest);
  }

  /**
   * update peer addresses of the member.
   *
   * @param memberID the id of member to update
   * @param endpoints the new endpoints for the member
   */
  @Override
  public ListenableFuture<MemberUpdateResponse> updateMember(long memberID,
      List<String> endpoints) {
    MemberUpdateRequest memberUpdateRequest = MemberUpdateRequest.newBuilder()
        .addAllPeerURLs(endpoints)
        .setID(memberID)
        .build();
    return stub.memberUpdate(memberUpdateRequest);
  }
}
