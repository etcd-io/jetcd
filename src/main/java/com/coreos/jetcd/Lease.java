package com.coreos.jetcd;

import com.coreos.jetcd.internal.impl.CloseableClient;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.lease.LeaseRevokeResponse;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.options.LeaseOption;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of KeepAlive talking to etcd.
 */
public interface Lease extends CloseableClient {

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
   * keep alive one lease only once.
   *
   * @param leaseId id of lease to keep alive once
   * @return The keep alive response
   */
  CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId);

  /**
   * retrieves the lease information of the given lease ID.
   *
   * @param leaseId id of lease
   * @param leaseOption LeaseOption
   * @return LeaseTimeToLiveResponse wrapped in CompletableFuture
   */
  CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId,
      LeaseOption leaseOption);

  /**
   * keep the given lease alive forever.
   *
   * @param leaseId lease to be keep alive forever.
   * @return a KeepAliveListener that listens for KeepAlive responses.
   */
  KeepAliveListener keepAlive(long leaseId);

  /**
   * KeepAliveListener listens for LeaseKeepAliveResponse of a given leaseID.
   */
  interface KeepAliveListener {

    /**
     * @return listen blocks until it receives a LeaseKeepAliveResponse.
     * @throws com.coreos.jetcd.exception.ClosedClientException if Lease client has been closed.
     * @throws com.coreos.jetcd.exception.ClosedKeepAliveListenerException if listener has been
     *        closed.
     * @throws InterruptedException if listen is interrupted.
     * @throws com.coreos.jetcd.exception.EtcdException if KeepAliveListener encounters client side
     *        and server side errors.
     */
    LeaseKeepAliveResponse listen() throws InterruptedException;

    /**
     * close KeepAliveListener. When all KeepAliveListeners for a given lease id are closed,
     * keep alive for that lease id will be stopped.
     *
     * <p>close() must be called to release resources of KeepAliveListener.
     */
    void close();
  }
}
