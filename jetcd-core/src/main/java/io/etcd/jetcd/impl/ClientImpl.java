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

package io.etcd.jetcd.impl;

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.Election;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.support.MemorizingClientSupplier;

/**
 * Etcd Client.
 */
public final class ClientImpl implements Client {

    private final ClientConnectionManager connectionManager;
    private final MemorizingClientSupplier<KV> kvClient;
    private final MemorizingClientSupplier<Auth> authClient;
    private final MemorizingClientSupplier<Maintenance> maintenanceClient;
    private final MemorizingClientSupplier<Cluster> clusterClient;
    private final MemorizingClientSupplier<Lease> leaseClient;
    private final MemorizingClientSupplier<Watch> watchClient;
    private final MemorizingClientSupplier<Lock> lockClient;
    private final MemorizingClientSupplier<Election> electionClient;

    public ClientImpl(ClientBuilder clientBuilder) {
        this.connectionManager = new ClientConnectionManager(clientBuilder.copy());
        this.kvClient = new MemorizingClientSupplier<>(() -> new KVImpl(this.connectionManager));
        this.authClient = new MemorizingClientSupplier<>(() -> new AuthImpl(this.connectionManager));
        this.maintenanceClient = new MemorizingClientSupplier<>(() -> new MaintenanceImpl(this.connectionManager));
        this.clusterClient = new MemorizingClientSupplier<>(() -> new ClusterImpl(this.connectionManager));
        this.leaseClient = new MemorizingClientSupplier<>(() -> new LeaseImpl(this.connectionManager));
        this.watchClient = new MemorizingClientSupplier<>(() -> new WatchImpl(this.connectionManager));
        this.lockClient = new MemorizingClientSupplier<>(() -> new LockImpl(this.connectionManager));
        this.electionClient = new MemorizingClientSupplier<>(() -> new ElectionImpl(this.connectionManager));
    }

    @Override
    public Auth getAuthClient() {
        return authClient.get();
    }

    @Override
    public KV getKVClient() {
        return kvClient.get();
    }

    @Override
    public Cluster getClusterClient() {
        return clusterClient.get();
    }

    @Override
    public Maintenance getMaintenanceClient() {
        return maintenanceClient.get();
    }

    @Override
    public Lease getLeaseClient() {
        return leaseClient.get();
    }

    @Override
    public Watch getWatchClient() {
        return watchClient.get();
    }

    @Override
    public Lock getLockClient() {
        return lockClient.get();
    }

    @Override
    public Election getElectionClient() {
        return electionClient.get();
    }

    @Override
    public synchronized void close() {
        authClient.close();
        kvClient.close();
        clusterClient.close();
        maintenanceClient.close();
        leaseClient.close();
        watchClient.close();
        lockClient.close();
        electionClient.close();

        connectionManager.close();
    }
}
