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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtcdClusterExtensionBuilderTest {

    @Test
    void builderShouldCreateExtension() {
        EtcdClusterExtension extension = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();

        assertThat(extension).isNotNull();
        assertThat(extension.clusterName()).isNotNull();
    }

    @Test
    void builderShouldAcceptCustomClusterName() {
        String clusterName = "test-cluster";
        EtcdClusterExtension extension = EtcdClusterExtension.builder()
            .withClusterName(clusterName)
            .withNodes(1)
            .build();

        assertThat(extension.clusterName()).isEqualTo(clusterName);
    }

    @Test
    void builderShouldRejectInvalidNodeCount() {
        assertThatThrownBy(() -> {
            EtcdClusterExtension.builder()
                .withNodes(0)
                .build();
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Node count must be positive");
    }

    @Test
    void builderShouldRejectNegativeNodeCount() {
        assertThatThrownBy(() -> {
            EtcdClusterExtension.builder()
                .withNodes(-1)
                .build();
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Node count must be positive");
    }

    @Test
    void builderShouldRejectNullClusterName() {
        assertThatThrownBy(() -> {
            EtcdClusterExtension.builder()
                .withClusterName(null)
                .build();
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cluster name cannot be null");
    }

    @Test
    void builderShouldRejectEmptyClusterName() {
        assertThatThrownBy(() -> {
            EtcdClusterExtension.builder()
                .withClusterName("")
                .build();
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cluster name cannot be null or empty");
    }

    @Test
    void builderShouldAcceptMultipleNodes() {
        EtcdClusterExtension extension = EtcdClusterExtension.builder()
            .withNodes(3)
            .build();

        assertThat(extension).isNotNull();
    }

    @Test
    void builderShouldAcceptPrefix() {
        EtcdClusterExtension extension = EtcdClusterExtension.builder()
            .withPrefix("my-prefix")
            .withNodes(1)
            .build();

        assertThat(extension).isNotNull();
    }

    @Test
    void builderShouldAcceptSslConfiguration() {
        EtcdClusterExtension extension = EtcdClusterExtension.builder()
            .withSsl(true)
            .withNodes(1)
            .build();

        assertThat(extension).isNotNull();
    }

    @Test
    void builderShouldAcceptDebugConfiguration() {
        EtcdClusterExtension extension = EtcdClusterExtension.builder()
            .withDebug(true)
            .withNodes(1)
            .build();

        assertThat(extension).isNotNull();
    }
}
