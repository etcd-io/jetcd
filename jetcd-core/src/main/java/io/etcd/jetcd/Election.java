/*
 * Copyright 2016-2020 The jetcd authors
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

import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.Beta;
import io.etcd.jetcd.election.CampaignResponse;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.election.ProclaimResponse;
import io.etcd.jetcd.election.ResignResponse;
import io.etcd.jetcd.support.CloseableClient;

/**
 * Interface of leader election client talking to etcd.
 */
@Beta
public interface Election extends CloseableClient {
    /**
     * Campaign waits to acquire leadership in an election, returning a leader key
     * representing the leadership if successful. The leader key can then be used
     * to issue new values on the election, transactionally guard API requests on
     * leadership still being held, and resign from the election.
     *
     * @param  electionName election name
     * @param  leaseId      lease identifier
     * @param  proposal     proposal
     * @return              the response
     */
    CompletableFuture<CampaignResponse> campaign(ByteSequence electionName, long leaseId, ByteSequence proposal);

    /**
     * Proclaim updates the leader's posted value with a new value. Only active
     * leader can change the value.
     *
     * @param  leaderKey leader key
     * @param  proposal  new proposal
     * @return           the response
     */
    CompletableFuture<ProclaimResponse> proclaim(LeaderKey leaderKey, ByteSequence proposal);

    /**
     * Returns the current election proclamation, if any.
     *
     * @param  electionName election name
     * @return              the response
     */
    CompletableFuture<LeaderResponse> leader(ByteSequence electionName);

    /**
     * Listens to election proclamations in-order as made by the election's
     * elected leaders.
     *
     * @param electionName election name
     * @param listener     listener
     */
    void observe(ByteSequence electionName, Listener listener);

    /**
     * Resign releases election leadership so other campaigners may acquire
     * leadership on the election.
     *
     * @param  leaderKey leader key
     * @return           the response
     */
    CompletableFuture<ResignResponse> resign(LeaderKey leaderKey);

    /**
     * Interface of leadership notification listener.
     */
    interface Listener {
        /**
         * Invoked on new leader elected.
         *
         * @param response the response.
         */
        void onNext(LeaderResponse response);

        /**
         * Invoked on errors.
         *
         * @param throwable the error.
         */
        void onError(Throwable throwable);

        /**
         * Invoked on completion.
         */
        void onCompleted();
    }
}
