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

package io.etcd.jetcd.lease;

import java.util.List;
import java.util.stream.Collectors;

import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.ByteSequence;

public class LeaseTimeToLiveResponse extends AbstractResponse<io.etcd.jetcd.api.LeaseTimeToLiveResponse> {

    private List<ByteSequence> keys;

    public LeaseTimeToLiveResponse(io.etcd.jetcd.api.LeaseTimeToLiveResponse response) {
        super(response, response.getHeader());
    }

    /**
     * @return the lease ID from the keep alive request.
     */
    public long getID() {
        return getResponse().getID();
    }

    /**
     * @return the remaining TTL in seconds for the lease; the lease will expire in under TTL+1 seconds.
     */
    public long getTTl() {
        return getResponse().getTTL();
    }

    /**
     * @return the initial granted time in seconds upon lease creation/renewal.
     */
    public long getGrantedTTL() {
        return getResponse().getGrantedTTL();
    }

    /**
     * @return the list of keys attached to this lease.
     */
    public synchronized List<ByteSequence> getKeys() {
        if (keys == null) {
            keys = getResponse().getKeysList().stream().map(ByteSequence::from).collect(Collectors.toList());
        }

        return keys;
    }
}
