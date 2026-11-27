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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdContainer;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import static io.etcd.jetcd.test.EtcdConstants.LOCALHOST;

/**
 * Testcontainer wrapper for a DNS server with SRV record support using dnsmasq.
 *
 * <p>
 * This container provides DNS SRV resolution for testing purposes, particularly
 * useful for testing etcd DNS SRV discovery. The container uses dnsmasq configured
 * with command-line arguments to serve SRV records.
 *
 * <p>
 * Usage examples:
 *
 * <pre>
 * // Single etcd node
 * EtcdContainer etcd = new EtcdContainer(Etcd.CONTAINER_IMAGE, "etcd0", List.of("etcd0"));
 * etcd.start();
 *
 * DnsSrvContainer dns = DnsSrvContainer.create()
 *     .withEtcdSrvRecord("_etcd._tcp.single.test.local", etcd)
 *     .withLogging(true);
 * dns.start();
 *
 * int dnsPort = dns.getDnsPort();
 *
 * // Multi-node cluster (autoconfiguration)
 * EtcdCluster cluster = Etcd.builder()
 *     .withNodes(3)
 *     .withClusterName("test-cluster")
 *     .build();
 * cluster.start();
 *
 * DnsSrvContainer dns = DnsSrvContainer.create()
 *     .withEtcdCluster("_etcd._tcp.cluster.local", cluster);
 * dns.start();
 *
 * // Manual configuration
 * DnsSrvContainer dns = DnsSrvContainer.create()
 *     .withSrvRecord("_etcd._tcp.test.local", EtcdConstants.LOCALHOST, 2379, 10, 100)
 *     .withSrvRecord("_etcd._tcp.test.local", EtcdConstants.LOCALHOST, 2380, 10, 50);
 * </pre>
 *
 * <p>
 * <strong>Important:</strong> When using with etcd containers, SRV records should
 * use localhost ({@code EtcdConstants.LOCALHOST}) as the target to avoid secondary A record lookups by
 * gRPC/Netty, which use the system DNS resolver instead of the custom dnsmasq instance.
 */
public class DnsSrvContainer extends GenericContainer<DnsSrvContainer> {
    private static final String DEFAULT_IMAGE = "4km3/dnsmasq:2.90-r3-alpine-3.22.2";
    private static final int DEFAULT_START_PORT = 15353;

    private final AtomicBoolean configured = new AtomicBoolean();
    private final List<SrvRecord> srvRecords = new ArrayList<>();
    private int hostPort;
    private int startPort = DEFAULT_START_PORT;
    private boolean enableLogging = true;
    private Consumer<OutputFrame> logConsumer;

    /**
     * Represents a DNS SRV record configuration.
     *
     * @param domain   the service domain (e.g., "_etcd._tcp.cluster.local")
     * @param target   the target host
     * @param port     the target port
     * @param priority the priority value for the SRV record
     * @param weight   the weight value for the SRV record
     */
    public record SrvRecord(
        String domain,
        String target,
        int port,
        int priority,
        int weight) {
        /**
         * Creates a new SRV record.
         *
         * @param domain   the service domain (e.g., "_etcd._tcp.cluster.local")
         * @param target   the target host
         * @param port     the target port
         * @param priority the priority (lower values have higher priority)
         * @param weight   the weight for load balancing
         */
        public SrvRecord {
            if (domain == null || domain.trim().isEmpty()) {
                throw new IllegalArgumentException("Domain cannot be null or empty");
            }
            if (target == null || target.trim().isEmpty()) {
                throw new IllegalArgumentException("Target cannot be null or empty");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            if (priority < 0) {
                throw new IllegalArgumentException("Priority cannot be negative");
            }
            if (weight < 0) {
                throw new IllegalArgumentException("Weight cannot be negative");
            }

        }
    }

    /**
     * Creates a new DNS SRV container with the specified Docker image.
     *
     * @param image the Docker image to use
     */
    public DnsSrvContainer(String image) {
        super(image);
    }

    /**
     * Creates a new DNS SRV container with the default dnsmasq image.
     *
     * @return a new DnsSrvContainer instance
     */
    public static DnsSrvContainer create() {
        return new DnsSrvContainer(DEFAULT_IMAGE);
    }

    /**
     * Adds a DNS SRV record with default priority (0) and weight (0).
     *
     * @param  domain the service domain (e.g., "_etcd._tcp.cluster.local")
     * @param  target the target host
     * @param  port   the target port
     * @return        this container
     */
    public DnsSrvContainer withSrvRecord(
        String domain,
        String target,
        int port) {
        return withSrvRecord(domain, target, port, 0, 0);
    }

    /**
     * Adds a DNS SRV record with custom priority and weight.
     *
     * @param  domain   the service domain (e.g., "_etcd._tcp.cluster.local")
     * @param  target   the target host
     * @param  port     the target port
     * @param  priority the priority (lower values have higher priority)
     * @param  weight   the weight for load balancing
     * @return          this container
     */
    public DnsSrvContainer withSrvRecord(
        String domain,
        String target,
        int port,
        int priority,
        int weight) {
        srvRecords.add(new SrvRecord(domain, target, port, priority, weight));
        return self();
    }

    /**
     * Adds a DNS SRV record pointing to an etcd container.
     * Uses localhost as the target to avoid secondary A record lookups.
     *
     * @param  domain        the service domain (e.g., "_etcd._tcp.single.test.local")
     * @param  etcdContainer the etcd container to point to
     * @return               this container
     */
    public DnsSrvContainer withEtcdSrvRecord(
        String domain,
        EtcdContainer etcdContainer) {
        if (!etcdContainer.isRunning()) {
            throw new IllegalStateException("EtcdContainer must be started before adding SRV record");
        }
        return withSrvRecord(domain, LOCALHOST, etcdContainer.getMappedPort(2379), 0, 0);
    }

    /**
     * Adds DNS SRV records for all containers in an etcd cluster.
     * Automatically creates one SRV record per cluster node.
     * Uses localhost as the target to avoid secondary A record lookups.
     *
     * @param  domain  the service domain (e.g., "_etcd._tcp.cluster.local")
     * @param  cluster the etcd cluster
     * @return         this container
     */
    public DnsSrvContainer withEtcdCluster(
        String domain,
        EtcdCluster cluster) {
        if (cluster == null) {
            throw new IllegalArgumentException("EtcdCluster cannot be null");
        }

        List<EtcdContainer> containers = cluster.containers();
        if (containers == null || containers.isEmpty()) {
            throw new IllegalStateException("EtcdCluster has no containers");
        }

        for (EtcdContainer container : containers) {
            if (!container.isRunning()) {
                throw new IllegalStateException(
                    "All containers in EtcdCluster must be started before adding SRV records");
            }
            withSrvRecord(domain, LOCALHOST, container.getMappedPort(2379), 0, 0);
        }

        return self();
    }

    /**
     * Enables or disables query logging in dnsmasq.
     *
     * @param  enabled true to enable logging, false to disable
     * @return         this container
     */
    public DnsSrvContainer withLogging(boolean enabled) {
        this.enableLogging = enabled;
        return self();
    }

    /**
     * Sets the starting port for finding an available DNS port.
     * The container will search for available ports starting from this value.
     *
     * @param  startPort the starting port (default: 15353)
     * @return           this container
     */
    public DnsSrvContainer withStartPort(int startPort) {
        if (startPort < 1 || startPort > 65535) {
            throw new IllegalArgumentException("Start port must be between 1 and 65535");
        }
        this.startPort = startPort;
        return self();
    }

    /**
     * Configure the log consumer for this container.
     *
     * @param  logConsumer the log consumer, or null to disable logging
     * @return             this container
     */
    @Override
    public DnsSrvContainer withLogConsumer(Consumer<OutputFrame> logConsumer) {
        this.logConsumer = logConsumer;
        return self();
    }

    /**
     * Returns the DNS port that the container is bound to on the host.
     *
     * @return                       the DNS port number
     * @throws IllegalStateException if the container is not running
     */
    public int getDnsPort() {
        if (!isRunning()) {
            throw new IllegalStateException("Container must be started to get DNS port");
        }
        return hostPort;
    }

    @Override
    protected void configure() {
        if (!configured.compareAndSet(false, true)) {
            return;
        }

        if (srvRecords.isEmpty()) {
            throw new IllegalStateException("At least one SRV record must be configured");
        }

        List<String> command = new ArrayList<>();
        // Use only the fixture's records and options, including when DNS is forwarded by a container VM.
        command.add("--conf-file=/dev/null");
        command.add("--no-daemon");
        command.add("--no-resolv");
        command.add("--no-hosts");

        if (enableLogging) {
            command.add("--log-queries");
        }

        for (SrvRecord record : srvRecords) {
            command.add(String.format(
                "--srv-host=%s,%s,%d,%d,%d",
                record.domain,
                record.target,
                record.port,
                record.priority,
                record.weight));
        }

        withCommand(command.toArray(new String[0]));
        if (logConsumer != null) {
            withLogConsumer(logConsumer);
        }

        hostPort = findAvailablePort(startPort);

        withCreateContainerCmdModifier(cmd -> {
            cmd.withExposedPorts(
                new ExposedPort(53, InternetProtocol.UDP));
            cmd.getHostConfig().withPortBindings(
                new PortBinding(
                    Ports.Binding.bindIpAndPort(LOCALHOST, hostPort),
                    new ExposedPort(53, InternetProtocol.UDP)));
        });
    }

    /**
     * Finds an available port starting from the specified port number.
     *
     * @param  startPort             the port to start searching from
     * @return                       an available port number
     * @throws IllegalStateException if no available port is found
     */
    private static int findAvailablePort(int startPort) {
        int port = startPort;
        while (!isPortAvailable(port)) {
            port++;
            if (port > 65535) {
                throw new IllegalStateException(
                    "No available ports found starting from " + startPort);
            }
        }
        return port;
    }

    /**
     * Checks if a port is available for binding.
     *
     * @param  port the port to check
     * @return      true if the port is available, false otherwise
     */
    @SuppressWarnings("PMD.UnusedLocalVariable")
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
