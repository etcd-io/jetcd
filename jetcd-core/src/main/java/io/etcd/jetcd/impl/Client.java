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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.Election;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.common.suppliers.CloseableSupplier;
import io.etcd.jetcd.common.suppliers.Suppliers;
import io.etcd.jetcd.grpc.GrpcService;

/**
 * Etcd Client implementation.
 */
public final class Client implements io.etcd.jetcd.Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    private final GrpcService grpcService;
    private final CloseableSupplier<KV> kvClient;
    private final CloseableSupplier<Auth> authClient;
    private final CloseableSupplier<Maintenance> maintenanceClient;
    private final CloseableSupplier<Cluster> clusterClient;
    private final CloseableSupplier<Lease> leaseClient;
    private final CloseableSupplier<Watch> watchClient;
    private final CloseableSupplier<Lock> lockClient;
    private final CloseableSupplier<Election> electionClient;

    /**
     * Creates a new Client instance from the builder.
     *
     * @param clientBuilder the client builder configuration
     */
    public Client(ClientBuilder clientBuilder) {
        this.grpcService = new GrpcService(clientBuilder.copy());
        this.kvClient = Suppliers.memoizingCloseable(() -> new KVClient(this.grpcService));
        this.authClient = Suppliers.memoizingCloseable(() -> new AuthClient(this.grpcService));
        this.maintenanceClient = Suppliers.memoizingCloseable(() -> new MaintenanceClient(this.grpcService));
        this.clusterClient = Suppliers.memoizingCloseable(() -> new ClusterClient(this.grpcService));
        this.leaseClient = Suppliers.memoizingCloseable(() -> new LeaseClient(this.grpcService));
        this.watchClient = Suppliers.memoizingCloseable(() -> new WatchClient(this.grpcService));
        this.lockClient = Suppliers.memoizingCloseable(() -> new LockClient(this.grpcService));
        this.electionClient = Suppliers.memoizingCloseable(() -> new ElectionClient(this.grpcService));
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
        try {
            closeAsync().get(15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOG.warn("Timeout waiting for Client to close");
        } catch (Exception e) {
            LOG.error("Error closing Client", e);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> closeAsync() {
        grpcService.beginShutdown();
        CompletableFuture<?>[] closeFutures = {
                closeQuietly(authClient),
                closeQuietly(kvClient),
                closeQuietly(clusterClient),
                closeQuietly(maintenanceClient),
                closeQuietly(leaseClient),
                closeQuietly(watchClient),
                closeQuietly(lockClient),
                closeQuietly(electionClient)
        };

        return CompletableFuture.allOf(closeFutures)
            .thenCompose(v -> grpcService.close());
    }

    private CompletableFuture<Void> closeQuietly(CloseableSupplier<?> supplier) {
        return CompletableFuture.runAsync(() -> {
            try {
                supplier.close();
            } catch (Exception e) {
                LOG.warn("Error closing supplier", e);
            }
        });
    }
}
