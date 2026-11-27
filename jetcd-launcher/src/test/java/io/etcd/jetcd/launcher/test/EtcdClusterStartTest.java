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

package io.etcd.jetcd.launcher.test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EtcdClusterStartTest {

    @Test
    public void testStartEtcd() throws Exception {
        try (EtcdCluster etcd = clusterBuilder().withClusterName(getClass().getSimpleName()).build()) {
            etcd.start();
        }
    }

    @Test
    public void testStartEtcdWithAdditionalArguments() throws Exception {
        try (EtcdCluster etcd = clusterBuilder().withClusterName(getClass().getSimpleName())
            .withAdditionalArgs("--max-txn-ops", "1024").build()) {
            etcd.start();
        }
    }

    @Test
    public void testContainerShouldMountDirectory() {
        try (EtcdCluster etcd = clusterBuilder().withClusterName(getClass().getSimpleName()).withMountedDataDirectory(true)
            .build()) {
            etcd.start();

            var containers = etcd.containers();

            containers.forEach(container -> assertTrue(container.hasDataDirectoryMounted(), "Data directory was not mounted"));
        }
    }

    @Test
    public void testContainerShouldNotMountDirectory() {
        try (EtcdCluster etcd = clusterBuilder().withClusterName(getClass().getSimpleName()).withMountedDataDirectory(false)
            .build()) {
            etcd.start();

            var containers = etcd.containers();

            containers.forEach(container -> assertFalse(container.hasDataDirectoryMounted(), "Data directory was mounted"));
        }
    }

    @Test
    public void testThreeNodeCluster() {
        try (EtcdCluster etcd = clusterBuilder()
            .withClusterName(getClass().getSimpleName() + "-3node")
            .withNodes(3)
            .build()) {

            etcd.start();

            // Verify cluster has 3 containers
            List<EtcdContainer> containers = etcd.containers();
            assertEquals(3, containers.size(), "Expected 3 containers");

            // Verify all containers are running
            containers.forEach(container -> assertNotNull(container.getContainerId(), "Container should be running"));

            // Verify 3 client endpoints
            List<URI> clientEndpoints = etcd.clientEndpoints();
            assertEquals(3, clientEndpoints.size(), "Expected 3 client endpoints");

            // Verify 3 peer endpoints
            List<URI> peerEndpoints = etcd.peerEndpoints();
            assertEquals(3, peerEndpoints.size(), "Expected 3 peer endpoints");

            // Verify cluster name
            assertEquals(getClass().getSimpleName() + "-3node", etcd.clusterName());
        }
    }

    @Test
    public void testFiveNodeCluster() {
        try (EtcdCluster etcd = clusterBuilder()
            .withClusterName(getClass().getSimpleName() + "-5node")
            .withNodes(5)
            .build()) {

            etcd.start();

            // Verify cluster has 5 containers
            assertEquals(5, etcd.containers().size(), "Expected 5 containers");
            assertEquals(5, etcd.clientEndpoints().size(), "Expected 5 client endpoints");
            assertEquals(5, etcd.peerEndpoints().size(), "Expected 5 peer endpoints");

            // Verify all endpoints are unique
            long uniqueClientEndpoints = etcd.clientEndpoints().stream().distinct().count();
            assertEquals(5, uniqueClientEndpoints, "All client endpoints should be unique");
        }
    }

    @Test
    public void testMultiNodeClusterRestart() throws InterruptedException {
        try (EtcdCluster etcd = clusterBuilder()
            .withClusterName(getClass().getSimpleName() + "-restart")
            .withNodes(3)
            .build()) {

            // Start cluster
            etcd.start();
            List<URI> originalEndpoints = etcd.clientEndpoints();
            assertEquals(3, originalEndpoints.size());

            // Restart cluster with delay
            etcd.restart(1, TimeUnit.SECONDS);

            // Verify cluster is running again
            List<URI> restartedEndpoints = etcd.clientEndpoints();
            assertEquals(3, restartedEndpoints.size(), "Expected 3 endpoints after restart");

            // Verify all containers are running
            etcd.containers()
                .forEach(container -> assertNotNull(container.getContainerId(), "Container should be running after restart"));
        }
    }

    @Test
    public void testMultiNodeClusterWithPrefix() {
        try (EtcdCluster etcd = clusterBuilder()
            .withClusterName(getClass().getSimpleName() + "-prefix")
            .withPrefix("test-")
            .withNodes(3)
            .build()) {

            etcd.start();

            // Verify cluster configuration
            assertEquals(3, etcd.containers().size());

            // Verify container names include prefix
            etcd.containers().forEach(container -> {
                String nodeName = container.node();
                assertTrue(nodeName.startsWith("test-etcd"), "Node name should start with prefix: " + nodeName);
            });
        }
    }

    private static Etcd.Builder clusterBuilder() {
        return Etcd.builder().withNetwork(Network.newNetwork());
    }

}
