/*
 * Copyright 2016-2020 The jetcd authors
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

import io.etcd.jetcd.support.MemoizingClientSupplier;

/**
 * Etcd Client.
 */
final class ClientImpl implements Client {

    private final ClientConnectionManager connectionManager;
    private final MemoizingClientSupplier<KV> kvClient;
    private final MemoizingClientSupplier<Auth> authClient;
    private final MemoizingClientSupplier<Maintenance> maintenanceClient;
    private final MemoizingClientSupplier<Cluster> clusterClient;
    private final MemoizingClientSupplier<Lease> leaseClient;
    private final MemoizingClientSupplier<Watch> watchClient;
    private final MemoizingClientSupplier<Lock> lockClient;
    private final MemoizingClientSupplier<Election> electionClient;

    public ClientImpl(ClientBuilder clientBuilder) {
        this.connectionManager = new ClientConnectionManager(clientBuilder.copy());
        this.kvClient = new MemoizingClientSupplier<>(() -> new KVImpl(this.connectionManager));
        this.authClient = new MemoizingClientSupplier<>(() -> new AuthImpl(this.connectionManager));
        this.maintenanceClient = new MemoizingClientSupplier<>(() -> new MaintenanceImpl(this.connectionManager));
        this.clusterClient = new MemoizingClientSupplier<>(() -> new ClusterImpl(this.connectionManager));
        this.leaseClient = new MemoizingClientSupplier<>(() -> new LeaseImpl(this.connectionManager));
        this.watchClient = new MemoizingClientSupplier<>(() -> new WatchImpl(this.connectionManager));
        this.lockClient = new MemoizingClientSupplier<>(() -> new LockImpl(this.connectionManager));
        this.electionClient = new MemoizingClientSupplier<>(() -> new ElectionImpl(this.connectionManager));
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
