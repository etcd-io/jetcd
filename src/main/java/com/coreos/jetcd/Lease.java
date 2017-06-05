package com.coreos.jetcd;

import com.coreos.jetcd.api.LeaseGrantResponse;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.api.LeaseRevokeResponse;
import com.coreos.jetcd.lease.NoSuchLeaseException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Interface of KeepAlive talking to etcd.
 */
public interface Lease {

  /**
   * New a lease with ttl value.
   *
   * @param ttl ttl value, unit seconds
   */
  CompletableFuture<LeaseGrantResponse> grant(long ttl);

  /**
   * revoke one lease and the key bind to this lease will be removed.
   *
   * @param leaseId id of the lease to revoke
   */
  CompletableFuture<LeaseRevokeResponse> revoke(long leaseId);

  /**
   * keep alive one lease in background.
   *
   * @param leaseId id of lease to set handler
   * @param leaseHandler the handler for the lease, this value can be null
   */
  void keepAlive(long leaseId, LeaseHandler leaseHandler);

  /**
   * cancel keep alive for lease in background.
   *
   * @param leaseId id of lease
   */
  void cancelKeepAlive(long leaseId) throws ExecutionException, InterruptedException;

  /**
   * keep alive one lease only once.
   *
   * @param leaseId id of lease to keep alive once
   * @return The keep alive response
   */
  CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId);

  /**
   * set LeaseHandler for lease.
   *
   * @param leaseId id of the lease to set handler
   * @param leaseHandler the handler for the lease
   * @throws NoSuchLeaseException if lease do not exist
   */
  void setLeaseHandler(long leaseId, LeaseHandler leaseHandler)
      throws NoSuchLeaseException;

  /**
   * Init the request stream to etcd.
   * start schedule to keep heartbeat to keep alive and remove dead leases.
   *
   * @throws IllegalStateException if the service is running already
   */
  void startKeepAliveService() throws IllegalStateException;

  /**
   * end the schedule for keep alive and remove dead leases.
   *
   * @throws IllegalStateException if the service is not running yet
   */
  void closeKeepAliveService() throws IllegalStateException;

  /**
   * It hints the state of the keep alive service.
   *
   * @return whether the keep alive service is running.
   */
  boolean isKeepAliveServiceRunning();

  /**
   * This interface is called by Etcd KeepAlive client to notify user about lease expiration and
   * exception.
   */
  interface LeaseHandler {

    /**
     * keepAliveResponse will be called when heartbeat keep alive call respond.
     */
    void onKeepAliveRespond(LeaseKeepAliveResponse keepAliveResponse);

    /**
     * onLeaseExpired will be called when any leases is expired and remove from keep alive task.
     */
    void onLeaseExpired(long leaseId);

    /**
     * onError will be called when keep alive encountered exception.
     */
    void onError(Throwable throwable);
  }

}
