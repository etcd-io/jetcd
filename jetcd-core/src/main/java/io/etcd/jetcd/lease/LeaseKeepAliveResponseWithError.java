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

import io.etcd.jetcd.common.exception.EtcdException;

/**
 * Wraps a lease keep alive response or an error.
 */
public class LeaseKeepAliveResponseWithError {

    /** The successful lease keep alive response, if any. */
    public LeaseKeepAliveResponse leaseKeepAliveResponse;
    /** The error, if any. */
    public EtcdException error;

    /**
     * Creates a new wrapper with a successful response.
     *
     * @param leaseKeepAliveResponse the lease keep alive response
     */
    public LeaseKeepAliveResponseWithError(LeaseKeepAliveResponse leaseKeepAliveResponse) {
        this.leaseKeepAliveResponse = leaseKeepAliveResponse;
    }

    /**
     * Creates a new wrapper with an error.
     *
     * @param e the exception
     */
    public LeaseKeepAliveResponseWithError(EtcdException e) {
        this.error = e;
    }
}
