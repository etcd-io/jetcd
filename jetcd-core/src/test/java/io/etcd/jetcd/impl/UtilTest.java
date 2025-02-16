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

package io.etcd.jetcd.impl;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.support.Errors;
import io.grpc.Status;
import io.grpc.StatusException;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class UtilTest {

    @Test
    public void testAuthStoreExpired() {
        Status authExpiredStatus = Status.INVALID_ARGUMENT
            .withDescription(Errors.ERROR_AUTH_STORE_OLD);
        Status status = Status.fromThrowable(new StatusException(authExpiredStatus));
        assertThat(Errors.isAuthStoreExpired(status)).isTrue();
    }

    @Test
    public void testAuthErrorIsRetryable() {
        Status authErrorStatus = Status.UNAUTHENTICATED
            .withDescription("etcdserver: invalid auth token");
        Status status = Status.fromThrowable(new StatusException(authErrorStatus));
        assertThat(Errors.isRetryableForNoSafeRedoOp(status)).isTrue();
        assertThat(Errors.isRetryableForSafeRedoOp(status)).isTrue();
    }

    @Test
    public void testUnavailableErrorIsRetryable() {
        Status status = Status.fromThrowable(new StatusException(Status.UNAVAILABLE));
        assertThat(Errors.isRetryableForNoSafeRedoOp(status)).isFalse();
        assertThat(Errors.isRetryableForSafeRedoOp(status)).isTrue();
    }
}
