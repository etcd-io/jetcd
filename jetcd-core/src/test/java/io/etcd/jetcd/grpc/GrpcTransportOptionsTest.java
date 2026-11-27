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

package io.etcd.jetcd.grpc;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.etcd.jetcd.Client;
import io.vertx.core.http.HttpClientOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrpcTransportOptionsTest {
    @Test
    void dedicatedConnectTimeoutOverridesOnlyTheTcpTimeout() {
        var supplied = new HttpClientOptions().setSsl(true).setUseAlpn(true).setConnectTimeout(1000)
            .setSslHandshakeTimeout(5);
        var builder = Client.builder("https://localhost:2379")
            .connectTimeout(Duration.ofMillis(200)).httpClientOptions(supplied);

        var effective = GrpcService.transportOptions(builder);
        assertThat(effective.getConnectTimeout()).isEqualTo(200);
        assertThat(effective.isSsl()).isTrue();
        assertThat(effective.isUseAlpn()).isTrue();
        assertThat(effective.getSslHandshakeTimeout()).isEqualTo(5);
        assertThat(supplied.getConnectTimeout()).isEqualTo(1000);
        assertThat(GrpcService.transportOptions(builder.connectTimeout(null)).getConnectTimeout()).isEqualTo(1000);
    }

    @Test
    void leavesTransportDefaultsUntilATimeoutIsSpecified() {
        var builder = Client.builder("http://localhost:2379");

        assertThat(GrpcService.transportOptions(builder)).isNull();
        assertThat(GrpcService.transportOptions(builder.connectTimeout(Duration.ofMillis(200))).getConnectTimeout())
            .isEqualTo(200);
        assertThat(GrpcService.transportOptions(builder.connectTimeout(Duration.ofNanos(1))).getConnectTimeout()).isOne();
        assertThat(GrpcService.transportOptions(builder.connectTimeout(Duration.ZERO)).getConnectTimeout()).isZero();
    }

    @Test
    void rejectsConnectTimeoutsOutsideTheSupportedRange() {
        var builder = Client.builder("http://localhost:2379");

        assertThatThrownBy(() -> builder.connectTimeout(Duration.ofNanos(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.connectTimeout(Duration.ofMillis((long) Integer.MAX_VALUE + 1)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.connectTimeout(Duration.ofSeconds(Long.MAX_VALUE)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
