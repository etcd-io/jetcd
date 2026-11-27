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

package io.etcd.jetcd;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import io.etcd.jetcd.resolver.ServiceResolver;
import io.etcd.jetcd.resolver.ServiceResolvers;
import io.vertx.core.net.SocketAddress;

/**
 * Etcd Client.
 *
 * <p>
 * The implementation may throw unchecked ConnectException or AuthFailedException on
 * initialization (or when invoking *Client methods if configured to initialize lazily).
 */
public interface Client extends AutoCloseable {

    /**
     * Returns the {@link Auth} client.
     *
     * @return the client.
     */
    Auth getAuthClient();

    /**
     * Returns the {@link KV} client.
     *
     * @return the client.
     */
    KV getKVClient();

    /**
     * Returns the {@link Cluster} client.
     *
     * @return the client.
     */
    Cluster getClusterClient();

    /**
     * Returns the {@link Maintenance} client.
     *
     * @return the client.
     */
    Maintenance getMaintenanceClient();

    /**
     * Returns the {@link Lease} client.
     *
     * @return the client.
     */
    Lease getLeaseClient();

    /**
     * Returns the {@link Watch} client.
     *
     * @return the client.
     */
    Watch getWatchClient();

    /**
     * Returns the {@link Lock} client.
     *
     * @return the client.
     */
    Lock getLockClient();

    /**
     * Returns the {@link Election} client.
     *
     * @return the client.
     */
    Election getElectionClient();

    @Override
    void close();

    /**
     * Asynchronously closes the client and all its resources.
     * Provides better control over shutdown timeout compared to synchronous close().
     *
     * @return CompletableFuture that completes when client is fully closed
     */
    java.util.concurrent.CompletableFuture<Void> closeAsync();

    /**
     * Returns a new {@link ClientBuilder} preconfigured with static addresses.
     *
     * @param  addresses etcd server addresses (host:port format)
     * @return           the builder.
     */
    static ClientBuilder builder(String... addresses) {
        return new ClientBuilder(ServiceResolvers.endpoints(addresses));
    }

    /**
     * Returns a new {@link ClientBuilder} preconfigured with static addresses from URIs.
     * Supports both varargs and array usage: builder(uri1, uri2) or builder(uriArray)
     *
     * @param  addresses etcd server URIs
     * @return           the builder.
     */
    static ClientBuilder builder(URI... addresses) {
        String[] addressStrings = Arrays.stream(addresses)
            .map(URI::toString)
            .toArray(String[]::new);
        return new ClientBuilder(ServiceResolvers.endpoints(addressStrings));
    }

    /**
     * Returns a new {@link ClientBuilder} preconfigured with static addresses from a collection of URIs.
     *
     * @param  addresses collection of etcd server URIs
     * @return           the builder.
     */
    static ClientBuilder builder(Collection<URI> addresses) {
        String[] addressStrings = addresses.stream()
            .map(URI::toString)
            .toArray(String[]::new);
        return new ClientBuilder(ServiceResolvers.endpoints(addressStrings));
    }

    /**
     * Returns a new {@link ClientBuilder} with a custom endpoint resolver.
     * Use this for advanced service discovery scenarios.
     *
     * @param  serviceResolver custom endpoint resolver
     * @return                 the builder.
     */
    static ClientBuilder builder(ServiceResolver<SocketAddress> serviceResolver) {
        return new ClientBuilder(serviceResolver);
    }
}
