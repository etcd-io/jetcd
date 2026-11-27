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

import java.util.stream.Collectors;

import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdContainer;
import io.etcd.jetcd.resolver.AbstractServiceResolver;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.SocketAddress;

/**
 * An endpoint resolver for testcontainers-based etcd clusters.
 */
public class EtcdClusterEndpointResolver extends AbstractServiceResolver<SocketAddress> {

    private static final int DEFAULT_PORT = 2379;
    private static final String DEFAULT_HOSTNAME = "etcd-test-cluster";

    /**
     * Creates an endpoint resolver for a testcontainers etcd cluster.
     * The resolver dynamically queries fresh endpoints on each resolution attempt,
     * ensuring it adapts to cluster restarts and port changes.
     * Uses default port (2379) and hostname ("etcd-test-cluster").
     *
     * @param  cluster                  the etcd cluster
     * @return                          an endpoint resolver
     * @throws IllegalArgumentException if cluster is null
     */
    public static EtcdClusterEndpointResolver create(EtcdCluster cluster) {
        return create(cluster, DEFAULT_PORT, DEFAULT_HOSTNAME);
    }

    /**
     * Creates an endpoint resolver for a testcontainers etcd cluster with custom target address.
     * The resolver dynamically queries fresh endpoints on each resolution attempt,
     * ensuring it adapts to cluster restarts and port changes.
     *
     * @param  cluster                  the etcd cluster
     * @param  port                     the target port for the resolver
     * @param  hostname                 the target hostname for the resolver
     * @return                          an endpoint resolver
     * @throws IllegalArgumentException if cluster is null, port is out of range, or hostname is null
     */
    public static EtcdClusterEndpointResolver create(EtcdCluster cluster, int port, String hostname) {
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster cannot be null");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        if (hostname == null) {
            throw new IllegalArgumentException("Hostname cannot be null");
        }

        // Create resolver that queries fresh endpoints on each resolution
        AddressResolver<SocketAddress> resolver = AddressResolver.mappingResolver(ignored -> {
            return cluster.containers().stream()
                .map(EtcdContainer::getClientAddress)
                .map(addr -> SocketAddress.inetSocketAddress(addr.getPort(), addr.getHostName()))
                .collect(Collectors.toList());
        });

        SocketAddress target = SocketAddress.inetSocketAddress(port, hostname);

        return new EtcdClusterEndpointResolver(resolver, target);
    }

    private EtcdClusterEndpointResolver(AddressResolver<SocketAddress> resolver, SocketAddress target) {
        super(resolver, target);
    }
}
