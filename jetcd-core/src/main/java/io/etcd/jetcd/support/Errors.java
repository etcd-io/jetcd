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

package io.etcd.jetcd.support;

import io.vertx.grpc.common.GrpcStatus;

/**
 * Utility class for error detection and classification.
 */
public final class Errors {
    /** Error message indicating no leader. */
    public static final String NO_LEADER_ERROR_MESSAGE = "etcdserver: no leader";
    /** Error message indicating invalid auth token. */
    public static final String INVALID_AUTH_TOKEN_ERROR_MESSAGE = "etcdserver: invalid auth token";
    /** Error message indicating permission denied. */
    public static final String PERMISSION_DENIED_ERROR_MESSAGE = "etcdserver: permission denied";
    /** Error message indicating old auth store revision. */
    public static final String ERROR_AUTH_STORE_OLD = "etcdserver: revision of auth store is old";

    private Errors() {
    }

    /**
     * Checks for an etcd response that rejected the token before executing the operation.
     *
     * @param  status  the gRPC status
     * @param  message the server's gRPC status message
     * @return         whether a new token can resolve the rejection
     */
    public static boolean isRefreshableAuthError(GrpcStatus status, String message) {
        return switch (status) {
            case UNAUTHENTICATED -> INVALID_AUTH_TOKEN_ERROR_MESSAGE.equals(message);
            case INVALID_ARGUMENT -> ERROR_AUTH_STORE_OLD.equals(message);
            default -> false;
        };
    }

    /**
     * Checks if the error status should halt reconnection attempts.
     * Transient errors (UNAVAILABLE, INTERNAL, UNKNOWN) allow reconnection.
     *
     * @param  status the gRPC status to check
     * @return        true if reconnection should be halted
     */
    public static boolean isHaltError(final GrpcStatus status) {
        // Allow reconnection for transient errors:
        // - UNAVAILABLE: server temporarily unavailable
        // - INTERNAL: internal server errors
        // - UNKNOWN: unexpected stream closures (e.g., during cluster restarts)
        return status != GrpcStatus.UNAVAILABLE
            && status != GrpcStatus.INTERNAL
            && status != GrpcStatus.UNKNOWN;
    }

    /**
     * Checks if the error status indicates a no-leader condition.
     *
     * @param  status the gRPC status to check
     * @return        true if it indicates a no-leader condition
     */
    public static boolean isNoLeaderError(final GrpcStatus status) {
        return status == GrpcStatus.UNAVAILABLE;
    }

    /**
     * Checks if the error message indicates a permission denied error.
     *
     * @param  message the error message to check
     * @return         true if permission denied error
     */
    public static boolean isPermissionDenied(String message) {
        return message != null && message.contains(PERMISSION_DENIED_ERROR_MESSAGE);
    }

    /**
     * Checks if the error message indicates an invalid auth token error.
     *
     * @param  message the error message to check
     * @return         true if invalid auth token error
     */
    public static boolean isInvalidAuthToken(String message) {
        return message != null && message.contains(INVALID_AUTH_TOKEN_ERROR_MESSAGE);
    }

    /**
     * Checks if the error message indicates an authentication error (permission denied or invalid token).
     *
     * @param  message the error message to check
     * @return         true if authentication error
     */
    public static boolean isAuthenticationError(String message) {
        return isPermissionDenied(message) || isInvalidAuthToken(message);
    }
}
