/*
 * Copyright 2016-2025 The jetcd authors
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

package io.etcd.jetcd.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtcdClusterEndpointResolverTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    @Test
    void createShouldReturnResolver() {
        EtcdClusterEndpointResolver resolver = EtcdClusterEndpointResolver.create(cluster.cluster());

        assertThat(resolver).isNotNull();
    }

    @Test
    void createWithCustomPortAndHostnameShouldReturnResolver() {
        EtcdClusterEndpointResolver resolver = EtcdClusterEndpointResolver.create(
            cluster.cluster(),
            2379,
            "custom-cluster-name");

        assertThat(resolver).isNotNull();
    }

    @Test
    void createShouldRejectNullCluster() {
        assertThatThrownBy(() -> {
            EtcdClusterEndpointResolver.create(null);
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cluster cannot be null");
    }

    @Test
    void createShouldRejectInvalidPort() {
        assertThatThrownBy(() -> {
            EtcdClusterEndpointResolver.create(cluster.cluster(), 0, "hostname");
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Port must be between 1 and 65535");
    }

    @Test
    void createShouldRejectPortTooLarge() {
        assertThatThrownBy(() -> {
            EtcdClusterEndpointResolver.create(cluster.cluster(), 65536, "hostname");
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Port must be between 1 and 65535");
    }

    @Test
    void createShouldRejectNullHostname() {
        assertThatThrownBy(() -> {
            EtcdClusterEndpointResolver.create(cluster.cluster(), 2379, null);
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hostname cannot be null");
    }
}
