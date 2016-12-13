package com.coreos.jetcd;

import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.data.EtcdHeader;
import com.coreos.jetcd.lease.Lease;
import com.coreos.jetcd.lease.NoSuchLeaseException;

import java.util.concurrent.CompletableFuture;
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
    CompletableFuture<Lease> grant(long ttl);

    /**
     * revoke one lease and the key bind to this lease will be removed
     *
     * @param lease lease to revoke
     * @return
     */
    CompletableFuture<EtcdHeader> revoke(Lease lease);

    /**
     * keep alive one lease in background
     *
     * @param lease            lease to set handler
     * @param etcdLeaseHandler the handler for the lease, this value can be null
     */
    void keepAlive(Lease lease, EtcdLeaseHandler etcdLeaseHandler);

    /**
     * cancel keep alive for lease in background
     *
     * @param lease lease
     */
    void cancelKeepAlive(Lease lease) throws ExecutionException, InterruptedException;

    /**
     * keep alive one lease only once
     *
     * @param lease lease to keep alive once
     * @return The keep alive response
     */
    CompletableFuture<EtcdHeader> keepAliveOnce(Lease lease);

    /**
     * set EtcdLeaseHandler for lease
     *
     * @param lease            the lease to set handler
     * @param etcdLeaseHandler the handler for the lease
     * @throws NoSuchLeaseException if lease do not exist
     */
    void setEtcdLeaseHandler(Lease lease, EtcdLeaseHandler etcdLeaseHandler) throws NoSuchLeaseException;

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
     *
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
