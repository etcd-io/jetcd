package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * Implementation of cluster client
 */
public class EtcdClusterImpl implements EtcdCluster {

    private ClusterGrpc.ClusterFutureStub clusterStub;

    public EtcdClusterImpl(ClusterGrpc.ClusterFutureStub stub){
        this.clusterStub = stub;
    }

    /**
     * lists the current cluster membership
     *
     * @return
     */
    @Override
    public ListenableFuture<MemberListResponse> listMember() {
        return clusterStub.memberList(
                MemberListRequest.getDefaultInstance());
    }

    /**
     * add a new member into the cluster
     *
     * @param endpoints the address of the new member
     * @return
     */
    @Override
    public ListenableFuture<MemberAddResponse> addMember(List<String> endpoints) {
        MemberAddRequest memberAddRequest = MemberAddRequest.newBuilder().addAllPeerURLs(endpoints).build();
        return clusterStub.memberAdd(memberAddRequest);
    }

    /**
     * removes an existing member from the cluster
     *
     * @param memberID the id of the member
     * @return
     */
    @Override
    public ListenableFuture<MemberRemoveResponse> removeMember(long memberID) {
        MemberRemoveRequest memberRemoveRequest = MemberRemoveRequest.newBuilder().setID(memberID).build();
        return clusterStub.memberRemove(memberRemoveRequest);
    }

    /**
     * update peer addresses of the member
     *
     * @param memberID the id of member to update
     * @param endpoints the new endpoints for the member
     * @return
     */
    @Override
    public ListenableFuture<MemberUpdateResponse> updateMember(long memberID, List<String> endpoints) {
        MemberUpdateRequest memberUpdateRequest = MemberUpdateRequest.newBuilder()
                .addAllPeerURLs(endpoints)
                .setID(memberID)
                .build();
        return clusterStub.memberUpdate(memberUpdateRequest);
    }
}
