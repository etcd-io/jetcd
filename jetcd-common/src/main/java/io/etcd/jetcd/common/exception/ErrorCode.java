/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.vertx.grpc.common.GrpcStatus;

/**
 * ErrorCode is a wrapper around gRPC Error code.
 *
 * <P>
 * Modification Notice:
 * This is a modification of ErrorCode.java from Google-cloud-spanner java api.
 * Updated to use Vert.x GrpcStatus instead of grpc-java Status.
 */
public enum ErrorCode {

    /** The operation was cancelled. */
    CANCELLED(GrpcStatus.CANCELLED),
    /** Unknown error occurred. */
    UNKNOWN(GrpcStatus.UNKNOWN),
    /** Client specified an invalid argument. */
    INVALID_ARGUMENT(GrpcStatus.INVALID_ARGUMENT),
    /** Deadline expired before operation could complete. */
    DEADLINE_EXCEEDED(GrpcStatus.DEADLINE_EXCEEDED),
    /** Some requested entity was not found. */
    NOT_FOUND(GrpcStatus.NOT_FOUND),
    /** An entity that we attempted to create already exists. */
    ALREADY_EXISTS(GrpcStatus.ALREADY_EXISTS),
    /** The caller does not have permission to execute the specified operation. */
    PERMISSION_DENIED(GrpcStatus.PERMISSION_DENIED),
    /** The request does not have valid authentication credentials. */
    UNAUTHENTICATED(GrpcStatus.UNAUTHENTICATED),
    /** Some resource has been exhausted. */
    RESOURCE_EXHAUSTED(GrpcStatus.RESOURCE_EXHAUSTED),
    /** Operation was rejected because the system is not in a state required for execution. */
    FAILED_PRECONDITION(GrpcStatus.FAILED_PRECONDITION),
    /** The operation was aborted. */
    ABORTED(GrpcStatus.ABORTED),
    /** Operation was attempted past the valid range. */
    OUT_OF_RANGE(GrpcStatus.OUT_OF_RANGE),
    /** Operation is not implemented or not supported. */
    UNIMPLEMENTED(GrpcStatus.UNIMPLEMENTED),
    /** Internal errors. */
    INTERNAL(GrpcStatus.INTERNAL),
    /** The service is currently unavailable. */
    UNAVAILABLE(GrpcStatus.UNAVAILABLE),
    /** Unrecoverable data loss or corruption. */
    DATA_LOSS(GrpcStatus.DATA_LOSS),;

    private static final Map<Integer, ErrorCode> errorByRpcCode;

    static {
        Map<Integer, ErrorCode> realMap = new LinkedHashMap<>();
        for (ErrorCode errorCode : values()) {
            realMap.put(errorCode.getCode(), errorCode);
        }
        errorByRpcCode = Collections.unmodifiableMap(realMap);
    }

    private final int code;

    ErrorCode(GrpcStatus status) {
        this.code = status.code;
    }

    int getCode() {
        return this.code;
    }

    /**
     * Returns the error code represents by {@code name}, or {@code defaultValue} if {@code name} does
     * not map to a known code.
     */
    static ErrorCode valueOf(String name, ErrorCode defaultValue) {
        try {
            return ErrorCode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the error code corresponding to a gRPC status, or {@code UNKNOWN} if not recognized.
     */
    static ErrorCode fromGrpcStatus(GrpcStatus status) {
        ErrorCode code = errorByRpcCode.get(status.code);
        return code == null ? UNKNOWN : code;
    }
}
