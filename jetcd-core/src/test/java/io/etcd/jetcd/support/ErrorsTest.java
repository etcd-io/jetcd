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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.vertx.grpc.common.GrpcStatus;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ErrorsTest {

    @Test
    public void recognizesEtcdTokenRejections() {
        assertThat(Errors.isRefreshableAuthError(GrpcStatus.INVALID_ARGUMENT, Errors.ERROR_AUTH_STORE_OLD)).isTrue();
        assertThat(Errors.isRefreshableAuthError(GrpcStatus.UNAUTHENTICATED, Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE)).isTrue();
    }

    @Test
    public void requiresBothTheStatusAndTheMessage() {
        assertThat(Errors.isRefreshableAuthError(GrpcStatus.INVALID_ARGUMENT, null)).isFalse();
        assertThat(Errors.isRefreshableAuthError(GrpcStatus.INVALID_ARGUMENT, "etcdserver: key is not provided")).isFalse();
        assertThat(Errors.isRefreshableAuthError(GrpcStatus.UNAUTHENTICATED, null)).isFalse();
        assertThat(Errors.isRefreshableAuthError(GrpcStatus.UNKNOWN, Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE)).isFalse();
        assertThat(Errors.isRefreshableAuthError(GrpcStatus.PERMISSION_DENIED, Errors.PERMISSION_DENIED_ERROR_MESSAGE))
            .isFalse();
    }
}
