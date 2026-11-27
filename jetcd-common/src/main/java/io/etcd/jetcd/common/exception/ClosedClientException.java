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

package io.etcd.jetcd.common.exception;

/**
 * Thrown when operations are attempted on a client that has been closed.
 * This exception indicates the client is no longer accepting requests.
 */
public class ClosedClientException extends EtcdException {

    /**
     * Creates a new ClosedClientException with the specified reason.
     *
     * @param reason the reason the client was closed
     */
    public ClosedClientException(String reason) {
        super(ErrorCode.CANCELLED, reason, null);
    }
}
