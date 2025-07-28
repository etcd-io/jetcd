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

package io.etcd.jetcd;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.etcd.jetcd.cluster.MemberAddResponse;
import io.etcd.jetcd.cluster.MemberListResponse;
import io.etcd.jetcd.cluster.MemberPromoteResponse;
import io.etcd.jetcd.cluster.MemberRemoveResponse;
import io.etcd.jetcd.cluster.MemberUpdateResponse;
import io.etcd.jetcd.support.CloseableClient;

/**
 * Interface of cluster client talking to etcd.
 */
public interface Cluster extends CloseableClient {

    /**
     * lists the current cluster membership.
     *
     * @return the response
     */
    CompletableFuture<MemberListResponse> listMember();

    /**
     * add a non-learner new member into the cluster.
     *
     * @param  peerAddrs the peer addresses of the new member
     * @return           the response
     */
    CompletableFuture<MemberAddResponse> addMember(List<URI> peerAddrs);

    /**
     * add a new member into the cluster.
     *
     * @param  peerAddrs the peer addresses of the new member
     * @param  isLearner whether the member is raft learner
     * @return           the response
     */
    CompletableFuture<MemberAddResponse> addMember(List<URI> peerAddrs, boolean isLearner);

    /**
     * removes an existing member from the cluster.
     *
     * @param  memberID the member to remove.
     * @return          the response
     */
    CompletableFuture<MemberRemoveResponse> removeMember(long memberID);

    /**
     * update peer addresses of the member.
     *
     * @param  memberID  the member id.
     * @param  peerAddrs the addresses.
     * @return           the response
     */
    CompletableFuture<MemberUpdateResponse> updateMember(long memberID, List<URI> peerAddrs);

    /**
     * Promotes a member from raft learner (non-voting) to raft voting member.
     *
     * @param  memberID the raft learner to be promoted to a raft voting member.
     * @return          the response
     */
    CompletableFuture<MemberPromoteResponse> promoteMember(long memberID);
}
