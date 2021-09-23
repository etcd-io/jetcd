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
     */
    Auth getAuthClient();

    /**
     * Returns the {@link KV} client.
     */
    KV getKVClient();

    /**
     * Returns the {@link Cluster} client.
     */
    Cluster getClusterClient();

    /**
     * Returns the {@link Maintenance} client.
     */
    Maintenance getMaintenanceClient();

    /**
     * Returns the {@link Lease} client.
     */
    Lease getLeaseClient();

    /**
     * Returns the {@link Watch} client.
     */
    Watch getWatchClient();

    /**
     * Returns the {@link Lock} client.
     */
    Lock getLockClient();

    /**
     * Returns the {@link Election} client.
     */
    Election getElectionClient();

    @Override
    void close();

    /**
     * Returns a new {@link ClientBuilder}.
     */
    static ClientBuilder builder() {
        return new ClientBuilder();
    }
}
