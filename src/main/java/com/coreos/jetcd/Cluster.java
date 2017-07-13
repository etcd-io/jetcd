package com.coreos.jetcd;

import com.coreos.jetcd.cluster.MemberAddResponse;
import com.coreos.jetcd.cluster.MemberListResponse;
import com.coreos.jetcd.cluster.MemberRemoveResponse;
import com.coreos.jetcd.cluster.MemberUpdateResponse;
import com.coreos.jetcd.internal.impl.CloseableClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of cluster client talking to etcd.
 */
public interface Cluster extends CloseableClient {

  /**
   * lists the current cluster membership.
   */
  CompletableFuture<MemberListResponse> listMember();

  /**
   * add a new member into the cluster.
   *
   * @param peerAddrs the peer addresses of the new member
   */
  CompletableFuture<MemberAddResponse> addMember(List<String> peerAddrs);

  /**
   * removes an existing member from the cluster.
   */
  CompletableFuture<MemberRemoveResponse> removeMember(long memberID);

  /**
   * update peer addresses of the member.
   */
  CompletableFuture<MemberUpdateResponse> updateMember(long memberID, List<String> peerAddrs);

}
