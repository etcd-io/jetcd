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

package io.etcd.jetcd.test;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdCluster;

/**
 * JUnit5 Extension to have etcd cluster in tests.
 *
 * <p>
 * This extension manages testcontainers-based etcd clusters for integration testing.
 * It supports two lifecycle modes:
 * <ul>
 * <li><strong>Class-level (@BeforeAll/@AfterAll):</strong> Cluster starts once before all tests
 * and remains running until all tests complete. Use this for faster test execution when
 * tests don't need isolation.</li>
 * <li><strong>Method-level (@BeforeEach/@AfterEach):</strong> Cluster starts/stops for each test.
 * Use this when tests need a fresh cluster state.</li>
 * </ul>
 *
 * <p>
 * <strong>Cluster Sharing:</strong> Multiple test classes can share the same cluster instance
 * by using the same cluster name. When using @BeforeAll, only use cluster sharing across test
 * classes that run in the same test suite execution to avoid premature cluster shutdown.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * {
 *     &#64;code
 *     public class MyTest {
 *         &#64;RegisterExtension
 *         public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
 *             .withNodes(3)
 *             .withClusterName("my-test-cluster")
 *             .build();
 *
 *         {@literal @}Test
 *         public void testSomething() {
 *             List&lt;URI&gt; endpoints = cluster.clientEndpoints();
 *             // use endpoints to connect
 *         }
 *     }
 * }
 * </pre>
 */
public class EtcdClusterExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    /**
     * Reference wrapper for shared cluster instances with reference counting.
     * Tracks how many extension instances are using each cluster to ensure
     * proper lifecycle management and cleanup.
     */
    private static class ClusterReference {
        final EtcdCluster cluster;
        final AtomicInteger refCount = new AtomicInteger(0);

        ClusterReference(EtcdCluster cluster) {
            this.cluster = cluster;
        }

        void incrementRef() {
            refCount.incrementAndGet();
        }

        boolean decrementRef() {
            return refCount.decrementAndGet() == 0;
        }
    }

    private static final Map<String, ClusterReference> CLUSTERS = new ConcurrentHashMap<>();

    private final String clusterName;
    private final EtcdCluster clusterTemplate;
    private final AtomicBoolean beforeAll;

    private EtcdClusterExtension(EtcdCluster clusterTemplate) {
        this.clusterName = clusterTemplate.clusterName();
        this.clusterTemplate = clusterTemplate;
        this.beforeAll = new AtomicBoolean();
    }

    /**
     * Returns the underlying etcd cluster for direct access to cluster operations.
     *
     * @return the etcd cluster instance, or null if not started
     */
    public EtcdCluster cluster() {
        return cluster(clusterName);
    }

    /**
     * Restarts all nodes in the cluster with a delay between restarts.
     * Useful for testing resilience and recovery scenarios.
     *
     * @param  delay            the delay between node restarts
     * @param  unit             the time unit of the delay
     * @throws RuntimeException if the restart is interrupted
     */
    public void restart(long delay, TimeUnit unit) {
        EtcdCluster cluster = cluster();
        if (cluster == null) {
            throw new IllegalStateException("Cluster not started");
        }
        try {
            cluster.restart(delay, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cluster restart was interrupted", e);
        }
    }

    /**
     * Returns the cluster name used to identify this cluster instance.
     *
     * @return the cluster name
     */
    public String clusterName() {
        return this.clusterName;
    }

    /**
     * Returns the client endpoints for connecting to the cluster.
     *
     * @return list of client endpoint URIs
     */
    public List<URI> clientEndpoints() {
        EtcdCluster cluster = cluster();
        if (cluster == null) {
            throw new IllegalStateException("Cluster not started");
        }
        return cluster.clientEndpoints();
    }

    /**
     * Returns the peer endpoints used for cluster member communication.
     *
     * @return list of peer endpoint URIs
     */
    public List<URI> peerEndpoints() {
        EtcdCluster cluster = cluster();
        if (cluster == null) {
            throw new IllegalStateException("Cluster not started");
        }
        return cluster.peerEndpoints();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        this.beforeAll.set(true);
        before(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        before(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        this.beforeAll.set(false);
        after(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        after(context);
    }

    /**
     * Lifecycle hook called before each test or before all tests.
     * Creates or retrieves a shared cluster instance and increments its reference count.
     *
     * @param context the extension context
     */
    protected synchronized void before(ExtensionContext context) {
        ClusterReference ref = CLUSTERS.computeIfAbsent(clusterName, k -> {
            ClusterReference newRef = new ClusterReference(clusterTemplate);
            newRef.cluster.start();
            return newRef;
        });
        ref.incrementRef();
    }

    /**
     * Lifecycle hook called after each test or after all tests.
     * Decrements the cluster reference count and closes the cluster if no longer needed.
     *
     * @param context the extension context
     */
    protected synchronized void after(ExtensionContext context) {
        if (!this.beforeAll.get()) {
            ClusterReference ref = CLUSTERS.get(clusterName);
            if (ref != null && ref.decrementRef()) {
                try {
                    ref.cluster.close();
                } finally {
                    CLUSTERS.remove(clusterName);
                }
            }
        }
    }

    /**
     * Retrieves a shared cluster by name. This allows multiple test classes
     * to reference the same cluster instance when using @BeforeAll lifecycle.
     *
     * @param  clusterName the name of the cluster to retrieve
     * @return             the cluster instance, or null if not found
     */
    public static EtcdCluster cluster(String clusterName) {
        ClusterReference ref = CLUSTERS.get(clusterName);
        return ref != null ? ref.cluster : null;
    }

    /**
     * Creates a new builder for configuring an etcd cluster extension.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring and creating an EtcdClusterExtension for JUnit tests.
     * Provides fluent API for setting up testcontainers-based etcd clusters with
     * various configurations including SSL, custom networks, and multiple nodes.
     * Each cluster gets a dedicated network when the caller leaves it unspecified.
     */
    public static class Builder {
        private final Etcd.Builder builder = Etcd.builder().withNetwork(Network.newNetwork());

        /**
         * Creates a new builder instance.
         */
        Builder() {
        }

        /**
         * Sets the cluster name for identification and shared cluster access.
         *
         * @param  clusterName the cluster name
         * @return             this builder
         */
        public Builder withClusterName(String clusterName) {
            builder.withClusterName(clusterName);
            return this;
        }

        /**
         * Sets the container name prefix for etcd nodes.
         *
         * @param  prefix the container name prefix
         * @return        this builder
         */
        public Builder withPrefix(String prefix) {
            builder.withPrefix(prefix);
            return this;
        }

        /**
         * Sets the number of etcd nodes in the cluster.
         * Use multiple nodes to test quorum, leader election, and cluster resilience.
         *
         * @param  nodes the number of nodes (must be positive)
         * @return       this builder
         */
        public Builder withNodes(int nodes) {
            builder.withNodes(nodes);
            return this;
        }

        /**
         * Enables or disables SSL/TLS for secure communication with the cluster.
         * When enabled, the cluster will use auto-generated certificates.
         *
         * @param  ssl true to enable SSL/TLS
         * @return     this builder
         */
        public Builder withSsl(boolean ssl) {
            builder.withSsl(ssl);
            return this;
        }

        /**
         * Enables debug mode with verbose logging.
         *
         * @param  debug true to enable debug mode
         * @return       this builder
         */
        public Builder withDebug(boolean debug) {
            builder.withDebug(debug);
            return this;
        }

        /**
         * Configure the log consumer for etcd containers.
         *
         * @param  logConsumer the log consumer
         * @return             this builder
         */
        public Builder withLogConsumer(Consumer<OutputFrame> logConsumer) {
            builder.withLogConsumer(logConsumer);
            return this;
        }

        /**
         * Enables container log output with a default SLF4J logger.
         * Container logs will be logged at INFO level for easier debugging.
         *
         * @return this builder
         */
        public Builder withContainerLogs() {
            builder.withContainerLogs();
            return this;
        }

        /**
         * Enables container log output with a default SLF4J logger.
         *
         * @param  verbose if true, logs STDOUT and STDERR separately; if false, logs all as INFO
         * @return         this builder
         */
        public Builder withContainerLogs(boolean verbose) {
            builder.withContainerLogs(verbose);
            return this;
        }

        /**
         * Adds additional command-line arguments to pass to etcd.
         *
         * @param  additionalArgs collection of additional arguments
         * @return                this builder
         */
        public Builder withAdditionalArgs(Collection<String> additionalArgs) {
            builder.withAdditionalArgs(additionalArgs);
            return this;
        }

        /**
         * Sets the etcd container image to use.
         *
         * @param  image the Docker image name (e.g., "quay.io/coreos/etcd:v3.5.0")
         * @return       this builder
         */
        public Builder withImage(String image) {
            builder.withImage(image);
            return this;
        }

        /**
         * Sets a custom testcontainers network for the cluster.
         * Useful when the etcd cluster needs to communicate with other containers
         * in the same test scenario.
         *
         * @param  network the testcontainers network
         * @return         this builder
         */
        public Builder withNetwork(Network network) {
            builder.withNetwork(network);
            return this;
        }

        /**
         * Enables mounting of the etcd data directory to the host filesystem.
         * This allows data to persist between container restarts, useful for testing
         * persistence and recovery scenarios.
         *
         * @param  mountDirectory true to mount the data directory
         * @return                this builder
         */
        public Builder withMountDirectory(boolean mountDirectory) {
            builder.withMountedDataDirectory(mountDirectory);
            return this;
        }

        /**
         * Sets the user for etcd authentication testing.
         *
         * @param  user the username
         * @return      this builder
         */
        public Builder withUser(String user) {
            builder.withUser(user);
            return this;
        }

        /**
         * Builds the EtcdClusterExtension with the configured options.
         *
         * @return the configured extension instance
         */
        public EtcdClusterExtension build() {
            return new EtcdClusterExtension(builder.build());
        }
    }
}
