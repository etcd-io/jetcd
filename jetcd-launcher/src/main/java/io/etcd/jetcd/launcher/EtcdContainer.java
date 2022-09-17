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
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class EtcdContainer extends GenericContainer<EtcdContainer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdContainer.class);

    private final AtomicBoolean configured;
    private final String node;
    private final Set<String> nodes;

    private String clusterToken;
    private boolean bindVolumn = true;
    private boolean ssl;
    private Path dataDirectory;
    private Collection<String> additionalArgs;

    public EtcdContainer(String image, String node, Collection<String> nodes) {
        super(image);

        this.configured = new AtomicBoolean();
        this.node = node;

        this.nodes = new HashSet<>(nodes);
        this.nodes.add(node);
    }

    public EtcdContainer withSll(boolean ssl) {
        this.ssl = ssl;
        return self();
    }

    public EtcdContainer withClusterToken(String clusterToken) {
        this.clusterToken = clusterToken;
        return self();
    }
    
    public EtcdContainer withBindVolumn(boolean bindVolumn){
        this.bindVolumn=bindVolumn;
        return self();
    }

    public EtcdContainer withAdditionalArgs(Collection<String> additionalArgs) {
        if (additionalArgs != null) {
            this.additionalArgs = Collections.unmodifiableCollection(new ArrayList<>(additionalArgs));
        }

        return self();
    }

    @Override
    protected void configure() {
        if (!configured.compareAndSet(false, true)) {
            return;
        }

        if(bindVolumn){
            dataDirectory = createDataDirectory(node);
            addFileSystemBind(dataDirectory.toString(), Etcd.ETCD_DATA_DIR, BindMode.READ_WRITE, SelinuxContext.SHARED);
        }

        withExposedPorts(Etcd.ETCD_PEER_PORT, Etcd.ETCD_CLIENT_PORT);
        withNetworkAliases(node);
        withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(node));
        withCommand(createCommand());

        waitingFor(Wait.forLogMessage(".*ready to serve client requests.*", 1));
    }

    private Path createDataDirectory(String name) {
        try {
            final String prefix = "jetcd_test_" + name + "_";
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                // https://github.com/etcd-io/jetcd/issues/489
                // Resolve symlink (/var -> /private/var) to don't fail for MacOS because of
                // docker thing with /var/folders
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

    private String[] createCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add("etcd");
        cmd.add("--name");
        cmd.add(node);
        cmd.add("--advertise-client-urls");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + Etcd.ETCD_CLIENT_PORT);
        cmd.add("--listen-client-urls");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + Etcd.ETCD_CLIENT_PORT);
        cmd.add("--data-dir");
        cmd.add(Etcd.ETCD_DATA_DIR);

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

    private static void deleteDataDirectory(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                LOGGER.error("Error deleting directory {}", dir, e);
            }
        }
    }

    @Override
    public void start() {
        LOGGER.debug("starting etcd container {} with command: {}", node, String.join(" ", getCommandParts()));

        try {
            super.start();
            execInContainer("chmod", "o+rwx", "-R", Etcd.ETCD_DATA_DIR);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        super.close();
        if(bindVolumn) {
            deleteDataDirectory(dataDirectory);
        }
    }

    public String node() {
        return this.node;
    }

    public InetSocketAddress getClientAddress() {
        return new InetSocketAddress(getContainerIpAddress(), getMappedPort(Etcd.ETCD_CLIENT_PORT));
    }

    public URI clientEndpoint() {
        return newURI(
            getContainerIpAddress(),
            getMappedPort(Etcd.ETCD_CLIENT_PORT));
    }

    public InetSocketAddress getPeerAddress() {
        return new InetSocketAddress(getContainerIpAddress(), getMappedPort(Etcd.ETCD_PEER_PORT));
    }

    public URI peerEndpoint() {
        return newURI(
            getContainerIpAddress(),
            getMappedPort(Etcd.ETCD_PEER_PORT));
    }

    private URI newURI(final String host, final int port) {
        try {
            return new URI(ssl ? "https" : "http", null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URISyntaxException should never happen here", e);
        }
    }
}
