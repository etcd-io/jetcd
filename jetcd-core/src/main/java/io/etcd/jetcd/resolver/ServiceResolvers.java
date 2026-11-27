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

package io.etcd.jetcd.resolver;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.etcd.jetcd.resolver.dnssrv.DnsSrvAddressResolver;
import io.etcd.jetcd.resolver.dnssrv.DnsSrvClientOptions;
import io.etcd.jetcd.support.Util;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.SocketAddress;

/**
 * Utility class providing factory methods for creating service resolvers.
 *
 * <p>
 * This class offers convenient ways to create service resolvers for various service discovery mechanisms:
 * </p>
 * <ul>
 * <li>Static endpoint lists via {@link #endpoints(String...)}</li>
 * <li>DNS SRV-based discovery via {@link #dnsSrv(String)}</li>
 * </ul>
 */
public final class ServiceResolvers {

    private ServiceResolvers() {
        // Utility class, no instantiation
    }

    /**
     * Creates a service resolver for static etcd endpoints.
     *
     * @param  addresses etcd server addresses (e.g., "http://localhost:2379")
     * @return           a service resolver for the specified addresses
     */
    public static ServiceResolver<SocketAddress> endpoints(String... addresses) {
        List<URI> uris = Stream.of(addresses)
            .map(URI::create)
            .collect(Collectors.toList());
        return Static.create(uris);
    }

    /**
     * Creates a service resolver for static etcd endpoints.
     *
     * @param  endpoints etcd server endpoint URIs
     * @return           a service resolver for the specified endpoints
     */
    public static ServiceResolver<SocketAddress> endpoints(URI... endpoints) {
        return Static.create(Arrays.asList(endpoints));
    }

    /**
     * Creates a service resolver that uses DNS SRV records for service discovery.
     *
     * @param  serviceName the DNS SRV service name (e.g., "_etcd._tcp.example.com")
     * @return             a service resolver that uses DNS SRV records
     */
    public static ServiceResolver<SocketAddress> dnsSrv(String serviceName) {
        return DnsSrv.create(serviceName);
    }

    /**
     * Creates a service resolver that uses DNS SRV records with a custom DNS server.
     *
     * @param  serviceName the DNS SRV service name (e.g., "_etcd._tcp.example.com")
     * @param  dnsServer   the DNS server hostname
     * @param  dnsPort     the DNS server port (usually 53)
     * @return             a service resolver that uses DNS SRV records
     */
    public static ServiceResolver<SocketAddress> dnsSrv(String serviceName, String dnsServer, int dnsPort) {
        return DnsSrv.create(serviceName, dnsServer, dnsPort);
    }

    /**
     * Static service resolver implementation.
     *
     * <p>
     * Resolves to a fixed list of etcd server endpoints. Use this when you have a known,
     * unchanging list of etcd servers.
     * </p>
     */
    public static final class Static extends AbstractServiceResolver<SocketAddress> {

        /**
         * Creates a static service resolver from a list of endpoint URIs.
         *
         * @param  endpoints list of etcd endpoint URIs
         * @return           a static service resolver
         */
        public static Static create(List<URI> endpoints) {
            List<SocketAddress> addresses = endpoints.stream()
                .map(Util::toSocketAddress)
                .collect(Collectors.toList());

            AddressResolver<SocketAddress> resolver = AddressResolver.mappingResolver(ignored -> addresses);
            SocketAddress target = SocketAddress.inetSocketAddress(2379, "etcd-cluster");

            return new Static(resolver, target);
        }

        private Static(AddressResolver<SocketAddress> resolver, SocketAddress target) {
            super(resolver, target);
        }
    }

    /**
     * DNS SRV service resolver implementation.
     *
     * <p>
     * Uses DNS SRV records to discover etcd endpoints dynamically with fully async resolution.
     * DNS queries are performed lazily on-demand with no blocking calls.
     * </p>
     *
     * <p>
     * DNS SRV records allow service discovery through DNS. For example,
     * "_etcd._tcp.example.com" would resolve to multiple etcd servers with
     * priority and weight information.
     * </p>
     *
     * <p>
     * Supports TTL-based automatic refresh to handle dynamic cluster topology changes.
     * </p>
     */
    public static final class DnsSrv extends AbstractServiceResolver<SocketAddress> {

        /**
         * Creates a DNS SRV resolver with default DNS server.
         *
         * @param  serviceName the DNS SRV service name (e.g., "_etcd._tcp.example.com")
         * @return             a service resolver that uses DNS SRV records
         */
        public static DnsSrv create(String serviceName) {
            DnsSrvClientOptions options = new DnsSrvClientOptions(serviceName);
            DnsSrvAddressResolver resolver = new DnsSrvAddressResolver(options);
            SocketAddress target = SocketAddress.inetSocketAddress(2379, serviceName);

            return new DnsSrv(resolver, target);
        }

        /**
         * Creates a DNS SRV resolver with a specific DNS server.
         *
         * @param  serviceName the DNS SRV service name (e.g., "_etcd._tcp.example.com")
         * @param  dnsServer   the DNS server hostname
         * @param  dnsPort     the DNS server port (usually 53)
         * @return             a service resolver that uses DNS SRV records
         */
        public static DnsSrv create(String serviceName, String dnsServer, int dnsPort) {
            DnsSrvClientOptions options = new DnsSrvClientOptions(serviceName)
                .setHost(dnsServer)
                .setPort(dnsPort);
            DnsSrvAddressResolver resolver = new DnsSrvAddressResolver(options);
            SocketAddress target = SocketAddress.inetSocketAddress(2379, serviceName);

            return new DnsSrv(resolver, target);
        }

        private DnsSrv(AddressResolver<SocketAddress> resolver, SocketAddress target) {
            super(resolver, target);
        }
    }
}
