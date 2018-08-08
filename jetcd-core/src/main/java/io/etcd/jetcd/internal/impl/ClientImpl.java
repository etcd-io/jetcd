/**
 * Copyright 2017 The jetcd authors
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

package io.etcd.jetcd.internal.impl;

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.Watch;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Etcd Client.
 */
public final class ClientImpl implements Client {

  private final AtomicReference<KV> kvClient;
  private final AtomicReference<Auth> authClient;
  private final AtomicReference<Maintenance> maintenanceClient;
  private final AtomicReference<Cluster> clusterClient;
  private final AtomicReference<Lease> leaseClient;
  private final AtomicReference<Watch> watchClient;
  private final AtomicReference<Lock> lockClient;
  private final ClientBuilder builder;
  private final ClientConnectionManager connectionManager;

  public ClientImpl(ClientBuilder clientBuilder) {
    // Copy the builder so external modifications won't affect this client impl.
    this.builder = clientBuilder.copy();

    this.kvClient = new AtomicReference<>();
    this.authClient = new AtomicReference<>();
    this.maintenanceClient = new AtomicReference<>();
    this.clusterClient = new AtomicReference<>();
    this.leaseClient = new AtomicReference<>();
    this.watchClient = new AtomicReference<>();
    this.lockClient = new AtomicReference<>();
    this.connectionManager = new ClientConnectionManager(this.builder);

    // If the client is not configured to be lazy, set up the managed connection and perform
    // authentication
    if (!clientBuilder.lazyInitialization()) {
      this.connectionManager.getChannel();
    }
  }

  @Override
  public Auth getAuthClient() {
    return newClient(authClient, AuthImpl::new);
  }

  @Override
  public KV getKVClient() {
    return newClient(kvClient, KVImpl::new);
  }

  @Override
  public Cluster getClusterClient() {
    return newClient(clusterClient, ClusterImpl::new);
  }

  @Override
  public Maintenance getMaintenanceClient() {
    return newClient(maintenanceClient, MaintenanceImpl::new);
  }

  @Override
  public Lease getLeaseClient() {
    return newClient(leaseClient, LeaseImpl::new);
  }

  @Override
  public Watch getWatchClient() {
    return newClient(watchClient, WatchImpl::new);
  }

  @Override
  public Lock getLockClient() {
    return newClient(lockClient, LockImpl::new);
  }

  @Override
  public synchronized void close() {
    Optional.ofNullable(authClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(kvClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(clusterClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(maintenanceClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(leaseClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(watchClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(lockClient.get()).ifPresent(CloseableClient::close);

    connectionManager.close();
  }

  /**
   * Create a new client instance.
   *
   * @param reference the atomic reference holding the instance
   * @param factory the factory to create the client
   * @param <T> the type of client
   * @return the client
   */
  private <T extends CloseableClient> T newClient(
      AtomicReference<T> reference, Function<ClientConnectionManager, T> factory) {

    T client = reference.get();

    if (client == null) {
      synchronized (reference) {
        client = reference.get();
        if (client == null) {
          client = factory.apply(connectionManager);
          reference.lazySet(client);
        }
      }
    }

    return client;
  }
}
