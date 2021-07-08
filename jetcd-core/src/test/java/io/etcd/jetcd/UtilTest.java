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

package io.etcd.jetcd;

import io.grpc.Status;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilTest {

    @Test
    public void testAuthErrorIsNotRetryable() {
        Status authErrorStatus = Status.UNAUTHENTICATED
            .withDescription("etcdserver: invalid auth token");
        assertThat(Util.isRetryable(new StatusException(authErrorStatus))).isTrue();
    }

    @Test
    public void testUnavailableErrorIsRetryable() {
        assertThat(Util.isRetryable(new StatusException(Status.UNAVAILABLE))).isTrue();
    }
}
