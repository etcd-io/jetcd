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

package io.etcd.jetcd.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import com.google.common.base.Strings;

/**
 * Factory for creating testcontainers-based etcd clusters for integration testing.
 * Provides a fluent builder API for configuring single or multi-node etcd clusters
 * with options for SSL, custom networks, and data persistence.
 */
public final class Etcd {
    /**
     * Default etcd container image.
     *
     * <p>
     * This version is updated periodically to track stable etcd releases.
     * The image can be overridden by setting the {@code ETCD_IMAGE} environment
     * variable, which is useful for testing against different etcd versions
     * without modifying code.
     *
     * @see #resolveContainerImage()
     */
    public static final String CONTAINER_IMAGE = "quay.io/coreos/etcd:v3.7.4";

    /**
     * Default port for etcd client communication.
     */
    public static final int ETCD_CLIENT_PORT = 2379;

    /**
     * Default port for etcd peer-to-peer cluster communication.
     */
    public static final int ETCD_PEER_PORT = 2380;

    /**
     * Default data directory path inside the container.
     */
    public static final String ETCD_DATA_DIR = "/data.etcd";

    private Etcd() {
    }

    /**
     * Resolves the etcd container image to use. Checks the ETCD_IMAGE environment
     * variable first, falling back to the default image if not set. This allows
     * testing against different etcd versions without code changes.
     *
     * @return the container image to use
     */
    private static String resolveContainerImage() {
        String image = System.getenv("ETCD_IMAGE");
        if (!Strings.isNullOrEmpty(image)) {
            return image;
        }
        return CONTAINER_IMAGE;
    }

    /**
     * Creates a new builder for configuring an etcd cluster.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring and creating etcd clusters.
     * Provides fluent API for setting up testcontainers-based etcd clusters with
     * options for multi-node setups, SSL, custom networks, and data persistence.
     */
    public static class Builder {
        private String image = resolveContainerImage();
        private String clusterName = UUID.randomUUID().toString();
        private String prefix;
        private int nodes = 1;
        private boolean ssl = false;
        private boolean debug = false;
        private Consumer<OutputFrame> logConsumer;
        private List<String> additionalArgs;
        private Network network;
        private boolean shouldMountDataDirectory = true;
        private String user;
        private long startupTimeout = Boolean.parseBoolean(System.getenv("CI")) ? 3 : 1;
        private TimeUnit startupTimeoutUnit = TimeUnit.MINUTES;

        /**
         * Creates a new Builder instance with default settings.
         */
        public Builder() {
        }

        /**
         * Sets the cluster name for identification.
         *
         * @param  clusterName the cluster name
         * @return             this builder
         */
        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        /**
         * Sets the container name prefix for etcd nodes.
         *
         * @param  prefix the container name prefix
         * @return        this builder
         */
        public Builder withPrefix(String prefix) {
            this.prefix = prefix;
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
            this.nodes = nodes;
            return this;
        }

        /**
         * Enables or disables SSL/TLS for secure communication with the cluster.
         * When enabled, the cluster will use auto-generated certificates from the classpath.
         *
         * @param  ssl true to enable SSL/TLS
         * @return     this builder
         */
        public Builder withSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        /**
         * Enables debug mode with verbose logging.
         *
         * @param  debug true to enable debug mode
         * @return       this builder
         */
        public Builder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Configure the log consumer for etcd containers.
         * By default, container logs are sent to SLF4J logger at INFO level.
         *
         * @param  logConsumer the log consumer, or null to disable logging
         * @return             this builder
         */
        public Builder withLogConsumer(Consumer<OutputFrame> logConsumer) {
            this.logConsumer = logConsumer;
            return this;
        }

        /**
         * Enables container log output with a default SLF4J logger.
         * Container logs will be prefixed with the container name for identification.
         * This is useful for debugging container startup issues.
         *
         * @return this builder
         */
        public Builder withContainerLogs() {
            return withContainerLogs(false);
        }

        /**
         * Enables container log output with a default SLF4J logger.
         * Container logs will be prefixed with the container name for identification.
         *
         * @param  verbose if true, logs STDOUT and STDERR separately; if false, logs all as INFO
         * @return         this builder
         */
        public Builder withContainerLogs(boolean verbose) {
            return withLogConsumer(createDefaultLogConsumer(verbose));
        }

        private Consumer<OutputFrame> createDefaultLogConsumer(boolean verbose) {
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("io.etcd.container");
            return frame -> {
                String message = frame.getUtf8String().trim();
                if (message.isEmpty()) {
                    return;
                }

                if (verbose) {
                    switch (frame.getType()) {
                        case STDOUT -> log.info("[STDOUT] {}", message);
                        case STDERR -> log.warn("[STDERR] {}", message);
                        case END -> {
                            // Ignore END frames
                        }
                    }
                } else {
                    log.info("{}", message);
                }
            };
        }

        /**
         * Adds additional command-line arguments to pass to etcd.
         *
         * @param  additionalArgs collection of additional arguments
         * @return                this builder
         */
        public Builder withAdditionalArgs(Collection<String> additionalArgs) {
            this.additionalArgs = Collections.unmodifiableList(new ArrayList<>(additionalArgs));
            return this;
        }

        /**
         * Adds additional command-line arguments to pass to etcd.
         *
         * @param  additionalArgs varargs of additional arguments
         * @return                this builder
         */
        public Builder withAdditionalArgs(String... additionalArgs) {
            this.additionalArgs = Collections.unmodifiableList(Arrays.asList(additionalArgs));
            return this;
        }

        /**
         * Sets the etcd container image to use.
         *
         * @param  image the Docker image name (e.g., "quay.io/coreos/etcd:v3.5.0")
         * @return       this builder
         */
        public Builder withImage(String image) {
            this.image = image;
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
            this.network = network;
            return this;
        }

        /**
         * Sets the timeout for cluster startup.
         * The cluster will wait up to this duration for all containers to start
         * before throwing an exception. Default is 1 minute.
         *
         * @param  timeout                  the timeout value (must be positive)
         * @param  unit                     the time unit for the timeout
         * @return                          this builder
         * @throws IllegalArgumentException if timeout is not positive
         */
        public Builder withStartupTimeout(long timeout, TimeUnit unit) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("Startup timeout must be positive, got: " + timeout);
            }
            if (unit == null) {
                throw new IllegalArgumentException("TimeUnit cannot be null");
            }
            this.startupTimeout = timeout;
            this.startupTimeoutUnit = unit;
            return this;
        }

        /**
         * Builds the EtcdCluster with the configured options.
         *
         * @return                          the configured cluster instance
         * @throws IllegalArgumentException if configuration is invalid
         */
        public EtcdCluster build() {
            if (nodes <= 0) {
                throw new IllegalArgumentException("Node count must be positive, got: " + nodes);
            }
            if (clusterName == null || clusterName.trim().isEmpty()) {
                throw new IllegalArgumentException("Cluster name cannot be null or empty");
            }
            if (image == null || image.trim().isEmpty()) {
                throw new IllegalArgumentException("Container image cannot be null or empty");
            }

            return new EtcdClusterImpl(
                image,
                clusterName,
                prefix,
                nodes,
                ssl,
                debug,
                logConsumer,
                additionalArgs,
                network,
                shouldMountDataDirectory,
                user,
                startupTimeout,
                startupTimeoutUnit);
        }

        /**
         * Enables mounting of the etcd data directory to the host filesystem.
         * This allows data to persist between container restarts, useful for testing
         * persistence and recovery scenarios.
         *
         * @param  shouldMountDataDirectory true to mount the data directory
         * @return                          this builder
         */
        public Builder withMountedDataDirectory(boolean shouldMountDataDirectory) {
            this.shouldMountDataDirectory = shouldMountDataDirectory;
            return this;
        }

        /**
         * Sets the user for running the etcd container process.
         * Useful for testing with specific user permissions.
         *
         * @param  user the user (e.g., "1000", "user:group")
         * @return      this builder
         */
        public Builder withUser(String user) {
            this.user = user;
            return this;
        }
    }
}
