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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class EtcdContainer implements AutoCloseable {
    public static final String ETCD_DOCKER_IMAGE_NAME = "gcr.io/etcd-development/etcd:v3.4.7";

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdCluster.class);
    private static final int ETCD_CLIENT_PORT = 2379;
    private static final int ETCD_PEER_PORT = 2380;
    private static final String ETCD_DATA_DIR = "/data.etcd";
    private final String endpoint;
    private final boolean ssl;
    private final FixedHostPortGenericContainer<?> container;
    private final LifecycleListener listener;
    private final Path dataDirectory;

    public EtcdContainer(Network network, LifecycleListener listener, boolean ssl, String clusterName, String endpoint,
        List<String> endpoints, String image, List<String> additionalArgs) {

        this.endpoint = endpoint;
        this.ssl = ssl;
        this.listener = listener;
        this.dataDirectory = createDataDirectory(endpoint);

        this.container = new FixedHostPortGenericContainer<>(image);
        this.container.withExposedPorts(ETCD_PEER_PORT);
        this.container.withFixedExposedPort(getAvailablePort(), ETCD_CLIENT_PORT);
        this.container.withNetwork(network);
        this.container.withNetworkAliases(endpoint);
        this.container.waitingFor(Wait.forLogMessage(".*ready to serve client requests.*", 1));
        this.container.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(endpoint));
        this.container.addFileSystemBind(dataDirectory.toString(), ETCD_DATA_DIR, BindMode.READ_WRITE, SelinuxContext.SHARED);

        List<String> cmd = new ArrayList<>();
        cmd.add("etcd");
        cmd.add("--name");
        cmd.add(endpoint);
        cmd.add("--advertise-client-urls");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + ETCD_CLIENT_PORT);
        cmd.add("--listen-client-urls");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + ETCD_CLIENT_PORT);
        cmd.add("--data-dir");
        cmd.add(ETCD_DATA_DIR);

        if (ssl) {
            this.container.withClasspathResourceMapping(
                "ssl/cert/" + endpoint + ".pem", "/etc/ssl/etcd/server.pem",
                BindMode.READ_ONLY,
                SelinuxContext.SHARED);

            this.container.withClasspathResourceMapping(
                "ssl/cert/" + endpoint + "-key.pem", "/etc/ssl/etcd/server-key.pem",
                BindMode.READ_ONLY,
                SelinuxContext.SHARED);

            cmd.add("--cert-file");
            cmd.add("/etc/ssl/etcd/server.pem");
            cmd.add("--key-file");
            cmd.add("/etc/ssl/etcd/server-key.pem");
        }

        if (endpoints.size() > 1) {
            cmd.add("--initial-advertise-peer-urls");
            cmd.add("http://" + endpoint + ":" + ETCD_PEER_PORT);
            cmd.add("--listen-peer-urls");
            cmd.add("http://0.0.0.0:" + ETCD_PEER_PORT);
            cmd.add("--initial-cluster-token");
            cmd.add(clusterName);
            cmd.add("--initial-cluster");
            cmd.add(endpoints.stream().map(e -> e + "=http://" + e + ":" + ETCD_PEER_PORT).collect(Collectors.joining(",")));
            cmd.add("--initial-cluster-state");
            cmd.add("new");
        }

        cmd.addAll(additionalArgs);

        if (!cmd.isEmpty()) {
            this.container.withCommand(cmd.toArray(new String[0]));
        }
    }

    private static Path createDataDirectory(String name) {
        try {
            final String prefix = "jetcd_test_" + name + "_";
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                // https://github.com/etcd-io/jetcd/issues/489
                // Resolve symlink (/var -> /private/var) to don't fail for Mac OS because of docker thing with /var/folders
                final FileAttribute<?> attribute = PosixFilePermissions
                    .asFileAttribute(EnumSet.allOf(PosixFilePermission.class));
                return Files.createTempDirectory(prefix, attribute).toRealPath();
            } else {
                return Files.createTempDirectory(prefix).toRealPath();
            }
        } catch (IOException e) {
            throw new ContainerLaunchException("Error creating data directory", e);
        }
    }

    private static void deleteDataDirectory(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                LOGGER.error("Error deleting directory {}", dir.toString(), e);
            }
        }
    }

    private static int getAvailablePort() {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress((InetAddress) null, 0), 1);
            return ss.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find free port", e);
        }
    }

    public void start() {
        LOGGER.debug("starting etcd container {} with command: {}", endpoint, String.join(" ", container.getCommandParts()));

        try {
            this.container.start();
            this.container.execInContainer("chmod", "o+rwx", "-R", ETCD_DATA_DIR);
            this.listener.started(this);
        } catch (Exception exception) {
            this.listener.failedToStart(this, exception);
        }
    }

    public void restart() {
        LOGGER.debug("restarting etcd container {} with command: {}", endpoint, String.join(" ", container.getCommandParts()));

        this.container.stop();
        this.container.start();
    }

    @Override
    public void close() {
        if (this.container != null) {
            this.container.stop();
        }

        deleteDataDirectory(dataDirectory);
    }

    // ****************
    // helpers
    // ****************

    public URI clientEndpoint() {
        return newURI(
            container.getContainerIpAddress(),
            container.getMappedPort(ETCD_CLIENT_PORT));
    }

    public URI peerEndpoint() {
        return newURI(
            container.getContainerIpAddress(),
            container.getMappedPort(ETCD_PEER_PORT));
    }

    private URI newURI(final String host, final int port) {
        try {
            return new URI(ssl ? "https" : "http", null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URISyntaxException should never happen here", e);
        }
    }

    interface LifecycleListener {
        void started(EtcdContainer container);

        void failedToStart(EtcdContainer container, Exception exception);

        void stopped(EtcdContainer container);
    }
}
