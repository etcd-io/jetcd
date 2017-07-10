package com.coreos.jetcd;

import com.coreos.jetcd.api.MemberAddResponse;
import com.coreos.jetcd.api.MemberListResponse;
import com.coreos.jetcd.api.MemberRemoveResponse;
import com.coreos.jetcd.api.MemberUpdateResponse;
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
   * @param endpoints the address of the new member
   */
  CompletableFuture<MemberAddResponse> addMember(List<String> endpoints);

  /**
   * removes an existing member from the cluster.
   */
  CompletableFuture<MemberRemoveResponse> removeMember(long memberID);

  /**
   * update peer addresses of the member.
   */
  CompletableFuture<MemberUpdateResponse> updateMember(long memberID, List<String> endpoints);

}
