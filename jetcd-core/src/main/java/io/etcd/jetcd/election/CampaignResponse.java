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

package io.etcd.jetcd.election;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.impl.AbstractResponse;

public class CampaignResponse extends AbstractResponse<io.etcd.jetcd.api.CampaignResponse> {
    private final LeaderKey leaderKey;

    public CampaignResponse(io.etcd.jetcd.api.CampaignResponse response) {
        super(response, response.getHeader());

        this.leaderKey = new LeaderKey(
            ByteSequence.from(getResponse().getLeader().getName()),
            ByteSequence.from(getResponse().getLeader().getKey()),
            getResponse().getLeader().getRev(),
            getResponse().getLeader().getLease()
        );
    }

    /**
     * Returns the resources used for holding leadership of the election.
     */
    public LeaderKey getLeader() {
        return leaderKey;
    }
}
