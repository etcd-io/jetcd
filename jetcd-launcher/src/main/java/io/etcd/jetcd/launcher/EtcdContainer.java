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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * Testcontainer wrapper for a single etcd node.
 * Supports configuration for SSL, clustering, custom networks, and data persistence.
 * Can be used standalone or as part of an {@link EtcdCluster}.
 */
public class EtcdContainer extends GenericContainer<EtcdContainer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdContainer.class);

    private final AtomicBoolean configured;
    private final String node;
    private final Set<String> nodes;

    private String clusterToken;
    private boolean ssl;
    private boolean debug;
    private Path dataDirectory;
    private Collection<String> additionalArgs;
    private boolean shouldMountDataDirectory = true;
    private String user;
    private Consumer<OutputFrame> logConsumer;

    /**
     * Creates a new etcd container.
     *
     * @param image the Docker image to use
     * @param node  the node name for this container
     * @param nodes all node names in the cluster (for multi-node setups)
     */
    public EtcdContainer(String image, String node, Collection<String> nodes) {
        super(image);

        this.configured = new AtomicBoolean();
        this.node = node;

        this.nodes = new HashSet<>(nodes);
        this.nodes.add(node);
    }

    /**
     * Enables or disables SSL/TLS for secure communication.
     * When enabled, uses auto-generated certificates from the classpath.
     *
     * @param  ssl true to enable SSL/TLS
     * @return     this container
     */
    public EtcdContainer withSsl(boolean ssl) {
        this.ssl = ssl;
        return self();
    }

    /**
     * Enables debug mode with verbose logging.
     *
     * @param  debug true to enable debug mode
     * @return       this container
     */
    public EtcdContainer withDebug(boolean debug) {
        this.debug = debug;
        return self();
    }

    /**
     * Configure the log consumer for this container.
     *
     * @param  logConsumer the log consumer, or null to disable logging
     * @return             this container
     */
    @Override
    public EtcdContainer withLogConsumer(Consumer<OutputFrame> logConsumer) {
        this.logConsumer = logConsumer;
        return self();
    }

    /**
     * Enables mounting of the etcd data directory to the host filesystem.
     * This allows data to persist between container restarts.
     *
     * @param  shouldMountDataDirectory true to mount the data directory
     * @return                          this container
     */
    public EtcdContainer withShouldMountDataDirectory(boolean shouldMountDataDirectory) {
        this.shouldMountDataDirectory = shouldMountDataDirectory;
        return self();
    }

    /**
     * Sets the cluster token for initial cluster bootstrap.
     * Used to identify the cluster during initial setup.
     *
     * @param  clusterToken the cluster token
     * @return              this container
     */
    public EtcdContainer withClusterToken(String clusterToken) {
        this.clusterToken = clusterToken;
        return self();
    }

    /**
     * Adds additional command-line arguments to pass to the etcd process.
     * Useful for custom etcd configuration beyond the standard options.
     *
     * @param  additionalArgs collection of additional arguments
     * @return                this container
     */
    public EtcdContainer withAdditionalArgs(Collection<String> additionalArgs) {
        if (additionalArgs != null) {
            this.additionalArgs = Collections.unmodifiableCollection(new ArrayList<>(additionalArgs));
        }

        return self();
    }

    /**
     * Optional values are {@code [ user | user:group | uid | uid:gid | user:gid | uid:group ]}.
     * See <a href="https://docs.docker.com/engine/reference/run/#user">User</a> .
     *
     * @param  user Refer to {@link com.github.dockerjava.api.command.CreateContainerCmd#withUser(String)}
     * @return      self container.
     */
    public EtcdContainer withUser(String user) {
        this.user = user;
        return self();
    }

    @Override
    protected void configure() {
        if (!configured.compareAndSet(false, true)) {
            return;
        }

        if (shouldMountDataDirectory) {
            dataDirectory = createDataDirectory(node);
            addFileSystemBind(dataDirectory.toString(), Etcd.ETCD_DATA_DIR, BindMode.READ_WRITE, SelinuxContext.SHARED);
        }

        withExposedPorts(Etcd.ETCD_PEER_PORT, Etcd.ETCD_CLIENT_PORT);
        withNetworkAliases(node);
        if (logConsumer != null) {
            withLogConsumer(logConsumer);
        }
        withCommand(createCommand());
        withEnv("ETCD_LOG_LEVEL", this.debug ? "debug" : "info");
        withEnv("ETCD_LOGGER", "zap");

        String tempUser = this.user;
        if (tempUser == null) {
            tempUser = EtcdSupport.getHostUser();
        }
        if (tempUser != null) {
            String finalUser = tempUser;
            withCreateContainerCmdModifier(c -> c.withUser(finalUser));
        }

        if (ssl) {
            waitingFor(Wait.forHttps("/health").forPort(Etcd.ETCD_CLIENT_PORT).allowInsecure());
        } else {
            waitingFor(Wait.forHttp("/health").forPort(Etcd.ETCD_CLIENT_PORT));
        }
    }

    private Path createDataDirectory(String name) {
        try {
            final String prefix = "jetcd_test_" + name + "_";
            Path dir;
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                // https://github.com/etcd-io/jetcd/issues/489
                // Resolve symlink (/var -> /private/var) to don't fail for MacOS because of
                // docker thing with /var/folders
                // Use rwxrwx--- (770) instead of rwxrwxrwx (777) for better security
                final FileAttribute<?> attribute = PosixFilePermissions
                    .asFileAttribute(PosixFilePermissions.fromString("rwxrwx---"));
                dir = Files.createTempDirectory(prefix, attribute).toRealPath();
            } else {
                dir = Files.createTempDirectory(prefix).toRealPath();
            }
            EtcdSupport.registerDirectoryForCleanup(dir);
            return dir;
        } catch (IOException e) {
            throw new ContainerLaunchException("Error creating data directory", e);
        }
    }

    private String[] createCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add("etcd");
        cmd.add("--name");
        cmd.add(node);
        cmd.add("--advertise-client-urls");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + Etcd.ETCD_CLIENT_PORT);
        cmd.add("--listen-client-urls");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + Etcd.ETCD_CLIENT_PORT);

        if (shouldMountDataDirectory) {
            cmd.add("--data-dir");
            cmd.add(Etcd.ETCD_DATA_DIR);
        } else {
            cmd.add("--data-dir");
            cmd.add("/tmp/etcd-data");
        }

        if (ssl) {
            withClasspathResourceMapping(
                "ssl/cert/" + node + ".pem", "/etc/ssl/etcd/server.pem",
                BindMode.READ_ONLY,
                SelinuxContext.SHARED);

            withClasspathResourceMapping(
                "ssl/cert/" + node + "-key.pem", "/etc/ssl/etcd/server-key.pem",
                BindMode.READ_ONLY,
                SelinuxContext.SHARED);

            cmd.add("--cert-file");
            cmd.add("/etc/ssl/etcd/server.pem");
            cmd.add("--key-file");
            cmd.add("/etc/ssl/etcd/server-key.pem");
        }

        if (nodes.size() > 1) {
            cmd.add("--initial-advertise-peer-urls");
            cmd.add("http://" + node + ":" + Etcd.ETCD_PEER_PORT);
            cmd.add("--listen-peer-urls");
            cmd.add("http://0.0.0.0:" + Etcd.ETCD_PEER_PORT);

            cmd.add("--initial-cluster");
            cmd.add(nodes.stream().map(e -> e + "=http://" + e + ":" + Etcd.ETCD_PEER_PORT).collect(Collectors.joining(",")));
            cmd.add("--initial-cluster-state");
            cmd.add("new");

            if (clusterToken != null) {
                cmd.add("--initial-cluster-token");
                cmd.add(clusterToken);
            }
        }

        if (additionalArgs != null) {
            cmd.addAll(additionalArgs);
        }

        return cmd.toArray(new String[0]);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        LOGGER.info("Container {} is starting, ID: {}", node, containerInfo.getId());
        super.containerIsStarting(containerInfo);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        LOGGER.info("Container {} started successfully, ID: {}", node, containerInfo.getId());
        super.containerIsStarted(containerInfo);
    }

    @Override
    public void start() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting etcd container '{}' with command: {}", node, String.join(" ", getCommandParts()));
        LOGGER.debug("Container {} configuration: ssl={}, debug={}, shouldMountDataDirectory={}, network={}",
            node, ssl, debug, shouldMountDataDirectory, getNetwork());

        try {
            super.start();
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("Container '{}' started successfully in {}ms", node, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.error("Failed to start container '{}' after {}ms", node, duration, e);

            // Try to get container logs if available
            try {
                String logs = getLogs();
                if (logs != null && !logs.isEmpty()) {
                    LOGGER.error("Container '{}' logs:\n{}", node, logs);
                }
            } catch (Exception logException) {
                LOGGER.warn("Could not retrieve logs for failed container '{}'", node, logException);
            }

            throw e;
        }
    }

    @Override
    public void close() {
        super.close();
        if (dataDirectory != null) {
            EtcdSupport.unregisterDirectoryForCleanup(dataDirectory);
            EtcdSupport.deleteDataDirectory(dataDirectory);
        }
    }

    /**
     * Returns the node name for this container.
     *
     * @return the node name
     */
    public String node() {
        return this.node;
    }

    /**
     * Returns the client address for connecting to this etcd node.
     *
     * @return the client socket address (host and mapped port)
     */
    public InetSocketAddress getClientAddress() {
        return new InetSocketAddress(getHost(), getMappedPort(Etcd.ETCD_CLIENT_PORT));
    }

    /**
     * Returns the client endpoint URI for connecting to this etcd node.
     *
     * @return the client endpoint URI
     */
    public URI clientEndpoint() {
        return newURI(
            getHost(),
            getMappedPort(Etcd.ETCD_CLIENT_PORT));
    }

    /**
     * Returns the peer address used for cluster member communication.
     *
     * @return the peer socket address (host and mapped port)
     */
    public InetSocketAddress getPeerAddress() {
        return new InetSocketAddress(getHost(), getMappedPort(Etcd.ETCD_PEER_PORT));
    }

    /**
     * Returns the peer endpoint URI used for cluster member communication.
     *
     * @return the peer endpoint URI
     */
    public URI peerEndpoint() {
        return newURI(
            getHost(),
            getMappedPort(Etcd.ETCD_PEER_PORT));
    }

    private URI newURI(final String host, final int port) {
        try {
            return new URI(ssl ? "https" : "http", null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URISyntaxException should never happen here", e);
        }
    }

    /**
     * Checks if the data directory is mounted to the host filesystem.
     *
     * @return true if the data directory is mounted
     */
    public boolean hasDataDirectoryMounted() {
        return dataDirectory != null;
    }
}
