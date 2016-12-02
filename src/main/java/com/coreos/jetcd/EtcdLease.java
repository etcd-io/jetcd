package com.coreos.jetcd;

import com.coreos.jetcd.api.LeaseGrantResponse;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.api.LeaseRevokeResponse;
import com.coreos.jetcd.lease.NoSuchLeaseException;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Interface of Lease talking to etcd
 */
public interface EtcdLease {

    /**
     * New a lease with ttl value
     *
     * @param ttl ttl value, unit seconds
     * @return
     */
    ListenableFuture<LeaseGrantResponse> grant(long ttl);

    /**
     * revoke one lease and the key bind to this lease will be removed
     *
     * @param leaseId id of the lease to revoke
     * @return
     */
    ListenableFuture<LeaseRevokeResponse> revoke(long leaseId);

    /**
     * keep alive one lease in background
     *
     * @param leaseId          id of lease to set handler
     * @param etcdLeaseHandler the handler for the lease, this value can be null
     */
    void keepAlive(long leaseId, EtcdLeaseHandler etcdLeaseHandler);

    /**
     * cancel keep alive for lease in background
     *
     * @param leaseId          id of lease
    */
    void cancelKeepAlive(long leaseId) throws ExecutionException, InterruptedException;

    /**
     * keep alive one lease only once
     *
     * @param leaseId id of lease to keep alive once
     * @return The keep alive response
     */
    ListenableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId);

    /**
     * set EtcdLeaseHandler for lease
     *
     * @param leaseId          id of the lease to set handler
     * @param etcdLeaseHandler the handler for the lease
     * @throws NoSuchLeaseException if lease do not exist
     */
    void setEtcdLeaseHandler(long leaseId, EtcdLeaseHandler etcdLeaseHandler) throws NoSuchLeaseException;

    /**
     * Init the request stream to etcd
     * start schedule to keep heartbeat to keep alive and remove dead leases
     *
     * @throws IllegalStateException if the service is running already
     */
    void startKeepAliveService() throws IllegalStateException;

    /**
     * end the schedule for keep alive and remove dead leases
     *
     * @throws IllegalStateException if the service is not running yet
     */
    void closeKeepAliveService() throws IllegalStateException;

    /**
     * It hints the state of the keep alive service.
     * @return whether the keep alive service is running.
     */
    boolean isKeepAliveServiceRunning();

    /**
     * This interface is called by Etcd Lease client to notify user about lease expiration and exception
     */
    interface EtcdLeaseHandler {

        /**
         * keepAliveResponse will be called when heartbeat keep alive call respond.
         *
         * @param keepAliveResponse
         */
        void onKeepAliveRespond(LeaseKeepAliveResponse keepAliveResponse);

        /**
         * onLeaseExpired will be called when any leases is expired and remove from keep alive task.
         *
         * @param leaseId
         */
        void onLeaseExpired(long leaseId);

        /**
         * onError will be called when keep alive encountered exception
         *
         * @param throwable
         */
        void onError(Throwable throwable);
    }

}
