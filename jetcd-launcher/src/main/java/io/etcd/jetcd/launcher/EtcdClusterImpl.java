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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import static java.util.stream.Collectors.toList;

/**
 * Implementation of EtcdCluster that manages a cluster of etcd containers.
 * Handles lifecycle management, container orchestration, and cluster configuration.
 */
public class EtcdClusterImpl implements EtcdCluster {
    private static final Logger LOG = LoggerFactory.getLogger(EtcdClusterImpl.class);

    private final List<EtcdContainer> containers;
    private final String clusterName;
    private final List<String> endpoints;
    private final Network network;
    private final long startupTimeout;
    private final TimeUnit startupTimeoutUnit;

    /**
     * Creates a new etcd cluster implementation with the specified configuration.
     *
     * @param image                    the container image to use
     * @param clusterName              the cluster name for identification
     * @param prefix                   the prefix for container names
     * @param nodes                    the number of nodes in the cluster
     * @param ssl                      whether to enable SSL
     * @param debug                    whether to enable debug mode
     * @param logConsumer              consumer for container logs
     * @param additionalArgs           additional etcd arguments
     * @param network                  the Docker network to use
     * @param shouldMountDataDirectory whether to mount data directory
     * @param user                     the user to run etcd as
     * @param startupTimeout           the startup timeout value
     * @param startupTimeoutUnit       the startup timeout unit
     */
    public EtcdClusterImpl(
        String image,
        String clusterName,
        String prefix,
        int nodes,
        boolean ssl,
        boolean debug,
        Consumer<OutputFrame> logConsumer,
        Collection<String> additionalArgs,
        Network network,
        boolean shouldMountDataDirectory,
        String user,
        long startupTimeout,
        TimeUnit startupTimeoutUnit) {

        this.clusterName = clusterName;
        this.startupTimeout = startupTimeout;
        this.startupTimeoutUnit = startupTimeoutUnit;
        this.endpoints = IntStream.range(0, nodes)
            .mapToObj(i -> (prefix == null ? "etcd" : prefix + "etcd") + i)
            .collect(toList());

        // Store network reference for cleanup. If null, use Network.SHARED as default
        this.network = network != null ? network : Network.SHARED;

        this.containers = endpoints.stream()
            .map(e -> new EtcdContainer(image, e, endpoints)
                .withClusterToken(clusterName)
                .withSsl(ssl)
                .withDebug(debug)
                .withLogConsumer(logConsumer)
                .withAdditionalArgs(additionalArgs)
                .withNetwork(this.network)
                .withShouldMountDataDirectory(shouldMountDataDirectory)
                .withUser(user))
            .collect(toList());
    }

    @Override
    public void start() {
        long clusterStartTime = System.currentTimeMillis();
        LOG.info("Starting etcd cluster '{}' with {} node(s), timeout: {} {}",
            clusterName, containers.size(), startupTimeout, startupTimeoutUnit);
        LOG.debug("Node names: {}", endpoints);

        ExecutorService executor = Executors.newFixedThreadPool(containers.size());

        try {
            // Create futures with individual tracking
            CompletableFuture<?>[] futures = containers.stream()
                .map(container -> CompletableFuture.runAsync(() -> {
                    String nodeName = container.node();
                    long nodeStartTime = System.currentTimeMillis();
                    LOG.info("Starting node '{}' in cluster '{}'", nodeName, clusterName);
                    try {
                        container.start();
                        long nodeDuration = System.currentTimeMillis() - nodeStartTime;
                        LOG.info("Node '{}' started successfully in {}ms", nodeName, nodeDuration);
                    } catch (Exception e) {
                        long nodeDuration = System.currentTimeMillis() - nodeStartTime;
                        LOG.error("Node '{}' failed to start after {}ms: {}",
                            nodeName, nodeDuration, e.getMessage(), e);
                        throw e;
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).get(startupTimeout, startupTimeoutUnit);

            long clusterDuration = System.currentTimeMillis() - clusterStartTime;
            LOG.info("Successfully started etcd cluster '{}' in {}ms", clusterName, clusterDuration);

        } catch (ExecutionException | TimeoutException e) {
            long clusterDuration = System.currentTimeMillis() - clusterStartTime;
            LOG.error("Failed to start etcd cluster '{}' after {}ms", clusterName, clusterDuration, e);

            // Log which containers succeeded and which failed
            logContainerStates();

            cleanupAfterFailure();

            if (e instanceof TimeoutException) {
                String msg = String.format(
                    "Cluster '%s' startup timed out after %d %s (%dms elapsed). " +
                        "Consider increasing timeout with withStartupTimeout()",
                    clusterName, startupTimeout, startupTimeoutUnit, clusterDuration);
                LOG.error(msg);
                throw new EtcdClusterTimeoutException(msg, e);
            }

            Throwable cause = e.getCause();
            String msg = String.format(
                "Cluster '%s' failed to start after %dms: %s",
                clusterName, clusterDuration,
                cause != null ? cause.getMessage() : e.getMessage());
            throw new EtcdClusterStartException(msg, e);

        } catch (InterruptedException e) {
            long clusterDuration = System.currentTimeMillis() - clusterStartTime;
            LOG.warn("Etcd cluster '{}' startup was interrupted after {}ms", clusterName, clusterDuration);
            cleanupAfterFailure();
            Thread.currentThread().interrupt();
            throw new EtcdClusterStartException("Interrupted while starting cluster", e);

        } finally {
            executor.shutdownNow();
        }
    }

    private void logContainerStates() {
        LOG.info("Container states for cluster '{}':", clusterName);
        for (EtcdContainer container : containers) {
            try {
                boolean running = container.isRunning();
                String state = running ? "RUNNING" : "STOPPED/FAILED";
                LOG.info("  - Node '{}': {}", container.node(), state);

                if (!running) {
                    // Try to get failure information
                    try {
                        String logs = container.getLogs();
                        if (logs != null && !logs.isEmpty()) {
                            LOG.debug("  - Node '{}' logs:\n{}", container.node(), logs);
                        }
                    } catch (Exception logEx) {
                        LOG.debug("  - Could not retrieve logs for node '{}'", container.node());
                    }
                }
            } catch (Exception e) {
                LOG.warn("  - Node '{}': Unable to determine state", container.node(), e);
            }
        }
    }

    private void cleanupAfterFailure() {
        try {
            stop();
        } catch (Exception e) {
            LOG.warn("Failed to cleanup containers after startup failure", e);
        }
    }

    @Override
    public void stop() {
        LOG.info("Stopping etcd cluster '{}'", clusterName);
        List<Exception> failures = new ArrayList<>();
        for (EtcdContainer container : containers) {
            try {
                container.stop();
            } catch (Exception e) {
                LOG.warn("Failed to stop container {}", container.node(), e);
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            LOG.error("Failed to stop {} container(s) in cluster '{}'", failures.size(), clusterName);
        } else {
            LOG.info("Successfully stopped etcd cluster '{}'", clusterName);
        }
    }

    @Override
    public void close() {
        LOG.debug("Closing etcd cluster '{}'", clusterName);
        // Close containers first
        for (EtcdContainer container : containers) {
            container.close();
        }

        // Try to clean up network if it's not the shared one
        // Network.SHARED is managed by Testcontainers and should never be closed
        if (network != null && !Network.SHARED.equals(network)) {
            try {
                network.close();
                LOG.debug("Successfully closed network for cluster '{}'", clusterName);
            } catch (Exception e) {
                // Log but don't fail - cleanup is best-effort
                // This is a fallback for when Ryuk service is not active
                LOG.warn("Failed to cleanup network for cluster '{}': {}", clusterName, e.getMessage());
            }
        }
        LOG.debug("Closed etcd cluster '{}'", clusterName);
    }

    @Override
    public String clusterName() {
        return clusterName;
    }

    @Override
    public List<URI> clientEndpoints() {
        return containers.stream().map(EtcdContainer::clientEndpoint).collect(toList());
    }

    @Override
    public List<URI> peerEndpoints() {
        return containers.stream().map(EtcdContainer::peerEndpoint).collect(toList());
    }

    @Override
    public List<EtcdContainer> containers() {
        return Collections.unmodifiableList(containers);
    }
}
