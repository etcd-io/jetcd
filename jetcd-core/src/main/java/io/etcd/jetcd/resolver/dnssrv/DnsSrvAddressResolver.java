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

package io.etcd.jetcd.resolver.dnssrv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.endpoint.EndpointResolver;

/**
 * Fully async AddressResolver that uses DNS SRV records for service discovery.
 * Performs lazy on-demand resolution with no blocking calls.
 * <p>
 * Maintains a separate EndpointResolver for each Vertx instance to handle cases where
 * different Vertx instances are used.
 * <p>
 * DNS resolution happens asynchronously when addresses are actually needed,
 * with TTL-based automatic refresh.
 */
public final class DnsSrvAddressResolver implements AddressResolver<SocketAddress> {
    private final Map<Vertx, EndpointResolver<SocketAddress, ?, ?, ?>> resolverCache;
    private final DnsSrvClientOptions options;

    /**
     * Creates a new DnsSrvAddressResolver with the specified options.
     *
     * @param options the DNS SRV client options
     */
    public DnsSrvAddressResolver(DnsSrvClientOptions options) {
        this.resolverCache = new ConcurrentHashMap<>();
        this.options = options;
    }

    @Override
    public EndpointResolver<SocketAddress, ?, ?, ?> endpointResolver(Vertx vertx) {
        return resolverCache.computeIfAbsent(vertx, this::createEndpointResolver);
    }

    private EndpointResolver<SocketAddress, ?, ?, ?> createEndpointResolver(Vertx vertx) {
        return new DnsSrvEndpointResolver<>(vertx, options);
    }
}
