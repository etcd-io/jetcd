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

import io.grpc.Status;

public final class Errors {
    public static final String NO_LEADER_ERROR_MESSAGE = "etcdserver: no leader";
    public static final String INVALID_AUTH_TOKEN_ERROR_MESSAGE = "etcdserver: invalid auth token";
    public static final String ERROR_AUTH_STORE_OLD = "etcdserver: revision of auth store is old";

    private Errors() {
    }

    public static boolean isRetryable(Status status) {
        return Status.UNAVAILABLE.getCode().equals(status.getCode()) || isInvalidTokenError(status) || isAuthStoreExpired(status);
    }

    public static boolean isInvalidTokenError(Throwable e) {
        Status status = Status.fromThrowable(e);
        return isInvalidTokenError(status);
    }

    public static boolean isInvalidTokenError(Status status) {
        return (status.getCode() == Status.Code.UNAUTHENTICATED || status.getCode() == Status.Code.UNKNOWN)
            && INVALID_AUTH_TOKEN_ERROR_MESSAGE.equals(status.getDescription());
    }

    public static boolean isAuthStoreExpired(Throwable e) {
        Status status = Status.fromThrowable(e);
        return isAuthStoreExpired(status);
    }

    public static boolean isAuthStoreExpired(Status status) {
        return (status.getCode() == Status.Code.UNAUTHENTICATED || status.getCode() == Status.Code.INVALID_ARGUMENT)
            && ERROR_AUTH_STORE_OLD.equals(status.getDescription());
    }

    public static boolean isHaltError(final Status status) {
        return status.getCode() != Status.Code.UNAVAILABLE && status.getCode() != Status.Code.INTERNAL;
    }

    public static boolean isNoLeaderError(final Status status) {
        return status.getCode() == Status.Code.UNAVAILABLE && NO_LEADER_ERROR_MESSAGE.equals(status.getDescription());
    }
}
