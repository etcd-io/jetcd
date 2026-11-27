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

import io.etcd.jetcd.impl.AbstractResponse;

/**
 * Response from a lease grant operation.
 */
public class LeaseGrantResponse extends AbstractResponse<io.etcd.jetcd.api.LeaseGrantResponse> {

    /**
     * Creates a new LeaseGrantResponse from the gRPC response.
     *
     * @param response the gRPC lease grant response
     */
    public LeaseGrantResponse(io.etcd.jetcd.api.LeaseGrantResponse response) {
        super(response, response.getHeader());
    }

    /**
     * Returns the lease ID for the granted lease.
     *
     * @return the id.
     */
    public long getID() {
        return getResponse().getID();
    }

    /**
     * Returns the server chosen lease time-to-live in seconds.
     *
     * @return the ttl.
     */
    public long getTTL() {
        return getResponse().getTTL();
    }
}
