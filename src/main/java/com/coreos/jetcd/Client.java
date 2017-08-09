package com.coreos.jetcd;

/**
 * Etcd Client.
 *
 * <p>The implementation may throw unchecked ConnectException or AuthFailedException on
 *    initialization (or when invoking *Client methods if configured to initialize lazily).
 */
public interface Client {

  Auth getAuthClient();

  KV getKVClient();

  Cluster getClusterClient();

  Maintenance getMaintenanceClient();

  Lease getLeaseClient();

  Watch getWatchClient();

  void close();

  static ClientBuilder builder() {
    return new ClientBuilder();
  }
}
