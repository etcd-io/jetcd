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

package io.etcd.jetcd.common.exception;

import io.grpc.Status;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base exception type for all exceptions produced by the etcd service.
 */
public class EtcdException extends RuntimeException {

    private final ErrorCode code;

    EtcdException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = checkNotNull(code);
    }

    /**
     * Returns the error code associated with this exception.
     */
    public ErrorCode getErrorCode() {
        return code;
    }

    /**
     * Converts to Status
     */
    public Status toStatus() {
        return Status.fromCodeValue(getErrorCode().getCode()).withDescription(getMessage()).withCause(getCause());
    }
}
