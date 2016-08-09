package com.coreos.jetcd;

import com.coreos.jetcd.api.MemberAddResponse;
import com.coreos.jetcd.api.MemberListResponse;
import com.coreos.jetcd.api.MemberRemoveResponse;
import com.coreos.jetcd.api.MemberUpdateResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * Interface of cluster client talking to etcd
 */
public interface EtcdCluster {

    /**
     * lists the current cluster membership
     *
     * @return
     */
    ListenableFuture<MemberListResponse> listMember();

    /**
     * add a new member into the cluster
     *
     * @param endpoints the address of the new member
     * @return
     */
    ListenableFuture<MemberAddResponse> addMember(List<String> endpoints);

    /**
     * removes an existing member from the cluster
     *
     * @param memberID
     * @return
     */
    ListenableFuture<MemberRemoveResponse> removeMember(long memberID);

    /**
     * update peer addresses of the member
     *
     * @param memberID
     * @param endpoints
     * @return
     */
    ListenableFuture<MemberUpdateResponse> updateMember(long memberID, List<String> endpoints);

}
