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

import io.etcd.jetcd.AbstractResponse;

public class LeaseKeepAliveResponse extends AbstractResponse<io.etcd.jetcd.api.LeaseKeepAliveResponse> {

    public LeaseKeepAliveResponse(io.etcd.jetcd.api.LeaseKeepAliveResponse response) {
        super(response, response.getHeader());
    }

    /**
     * Returns the lease ID from the keep alive request.
     */
    public long getID() {
        return getResponse().getID();
    }

    /**
     * Returns the new time-to-live for the lease.
     */
    public long getTTL() {
        return getResponse().getTTL();
    }
}
