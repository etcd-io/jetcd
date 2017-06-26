package com.coreos.jetcd;

/**
 * Etcd Client.
 */
public interface Client {

  Auth getAuthClient();

  KV getKVClient();

  Cluster getClusterClient();

  Maintenance getMaintenanceClient();

  Lease getLeaseClient();

  Watch getWatchClient();

  void close();
}
