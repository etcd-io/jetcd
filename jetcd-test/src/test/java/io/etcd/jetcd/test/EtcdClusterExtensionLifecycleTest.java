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

class EtcdClusterExtensionLifecycleTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .withClusterName("lifecycle-test-cluster")
        .build();

    @Test
    void clusterShouldBeAccessibleAfterStart() {
        assertThat(cluster.cluster()).isNotNull();
        assertThat(cluster.clusterName()).isEqualTo("lifecycle-test-cluster");
    }

    @Test
    void clientEndpointsShouldBeAvailableAfterStart() {
        assertThat(cluster.clientEndpoints()).isNotEmpty();
    }

    @Test
    void peerEndpointsShouldBeAvailableAfterStart() {
        assertThat(cluster.peerEndpoints()).isNotEmpty();
    }

    @Test
    void staticClusterMethodShouldReturnSharedCluster() {
        assertThat(EtcdClusterExtension.cluster("lifecycle-test-cluster"))
            .isNotNull()
            .isSameAs(cluster.cluster());
    }

    @Test
    void staticClusterMethodShouldReturnNullForNonExistentCluster() {
        assertThat(EtcdClusterExtension.cluster("non-existent-cluster")).isNull();
    }

    @Test
    void extensionBeforeStartShouldThrowException() {
        EtcdClusterExtension notStarted = EtcdClusterExtension.builder()
            .withNodes(1)
            .withClusterName("not-started-cluster")
            .build();

        assertThatThrownBy(() -> {
            notStarted.clientEndpoints();
        }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cluster not started");
    }

    @Test
    void restartBeforeStartShouldThrowException() {
        EtcdClusterExtension notStarted = EtcdClusterExtension.builder()
            .withNodes(1)
            .withClusterName("restart-not-started-cluster")
            .build();

        assertThatThrownBy(() -> {
            notStarted.restart(1, java.util.concurrent.TimeUnit.SECONDS);
        }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cluster not started");
    }
}
