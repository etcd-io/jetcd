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
     * @return the {@link Auth} client.
     */
    Auth getAuthClient();

    /**
     * @return the {@link KV} client.
     */
    KV getKVClient();

    /**
     * @return the {@link Cluster} client.
     */
    Cluster getClusterClient();

    /**
     * @return the {@link Maintenance} client.
     */
    Maintenance getMaintenanceClient();

    /**
     * @return the {@link Lease} client.
     */
    Lease getLeaseClient();

    /**
     * @return the {@link Watch} client.
     */
    Watch getWatchClient();

    /**
     * @return the {@link Lock} client.
     */
    Lock getLockClient();

    /**
     * @return the {@link Election} client.
     */
    Election getElectionClient();

    @Override
    void close();

    /**
     * @return a new {@link ClientBuilder}.
     */
    static ClientBuilder builder() {
        return new ClientBuilder();
    }
}
