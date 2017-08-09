package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
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
  public synchronized void close() {
    Optional.ofNullable(authClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(kvClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(clusterClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(maintenanceClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(leaseClient.get()).ifPresent(CloseableClient::close);
    Optional.ofNullable(watchClient.get()).ifPresent(CloseableClient::close);

    connectionManager.close();
  }

  /**
   * Create a new client instance.
   *
   * @param reference the atomic reference holding the instance
   * @param factory the factory to create the client
   * @param <T> the type of client
   * @return the client
   * @throws AuthFailedException This may be caused as network reason, wrong address
   * @throws ConnectException This may be caused as wrong username or password
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
