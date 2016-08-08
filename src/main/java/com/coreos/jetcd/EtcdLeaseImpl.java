package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.lease.Lease;
import com.coreos.jetcd.lease.NoSuchLeaseException;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Implementation of lease client
 */
public class EtcdLeaseImpl implements EtcdLease {

    private final static int DEFAULT_TTL = 5000;
    private final static int DEFAULT_SCAN_PERIOD = 500;
    private final LeaseGrpc.LeaseFutureStub leaseFutureStub;
    private final LeaseGrpc.LeaseStub leaseStub;
    /**
     * gRPC channel
     */
    private ManagedChannel channel;
    /**
     * Timer schedule to send keep alive request
     */
    private ScheduledExecutorService keepAliveSchedule;
    private ScheduledFuture<?> scheduledFuture;
    private long scanPeriod;

    private Map<Long, Lease> keepAlives = new ConcurrentHashMap<>();

    /**
     * The first time interval
     */
    private long firstKeepAliveTimeOut = DEFAULT_TTL;

    /**
     * KeepAlive Request Stream, put request into this stream to keep the lease alive
     */
    private StreamObserver<LeaseKeepAliveRequest> keepAliveRequestStreamObserver;

    /**
     * KeepAlive Response Streamer, receive keep alive response from this stream and update the
     * nextKeepAliveTime and deadline of the leases.
     */
    private StreamObserver<LeaseKeepAliveResponse> keepAliveResponseStreamObserver;

    public EtcdLeaseImpl(final ManagedChannel channel) {
        /**
         * Init lease stub
         */
        this.channel = channel;
        this.leaseFutureStub = LeaseGrpc.newFutureStub(this.channel);
        this.leaseStub = LeaseGrpc.newStub(this.channel);
        this.scanPeriod = DEFAULT_SCAN_PERIOD;
    }

    /**
     * Init the request stream to etcd
     * start schedule to keep heartbeat to keep alive and remove dead leases
     *
     * @throws IllegalStateException if the service is running already
     */
    public void startKeepAliveService() {

        /**
         * This function is called by user, and it may be thread unsafe,
         * so we sync here.
         */
        synchronized (this) {
            if (isKeepAliveServiceRunning()) {
                throw new IllegalStateException("Lease keep alive service already start");
            }
            keepAliveResponseStreamObserver = new StreamObserver<LeaseKeepAliveResponse>() {
                @Override
                public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
                    processKeepAliveRespond(leaseKeepAliveResponse);
                    Lease lease = keepAlives.get(leaseKeepAliveResponse.getID());
                    if (lease != null && lease.isContainHandler()) {
                        lease.getEtcdLeaseHandler().onKeepAliveRespond(leaseKeepAliveResponse);
                    }
                }

                @Override
                public void onError(Throwable throwable) {}

                @Override
                public void onCompleted() {}
            };

            initRequestStream(keepAliveResponseStreamObserver);

            /**
             * Start heartbeat schedule to keep alive leases and remove dead leases
             */
            if (this.keepAliveSchedule == null) {
                this.keepAliveSchedule = Executors.newSingleThreadScheduledExecutor();
            }
            this.scheduledFuture = this.keepAliveSchedule.scheduleAtFixedRate(
                    () -> {
                        /**
                         * The keepAliveExecutor and deadLineExecutor will be sequentially executed in
                         * one thread.
                         */
                        keepAliveExecutor();
                        deadLineExecutor();
                    }, 0, this.scanPeriod, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * It hints the state of the keep alive service.
     *
     * @return whether the keep alive service is running.
     */
    @Override
    public boolean isKeepAliveServiceRunning() {
        /**
         * This function is called by user, and it may be thread unsafe,
         * so we sync here.
         */
        synchronized (this) {
            return this.scheduledFuture != null && !this.scheduledFuture.isCancelled();
        }
    }

    /**
     * New a lease with ttl value
     *
     * @param ttl ttl value, unit seconds
     * @return
     */
    @Override
    public ListenableFuture<LeaseGrantResponse> grant(long ttl) {
        LeaseGrantRequest leaseGrantRequest =
                LeaseGrantRequest.newBuilder()
                        .setTTL(ttl)
                        .build();
        return this.leaseFutureStub.leaseGrant(leaseGrantRequest);
    }

    /**
     * revoke one lease and the key bind to this lease will be removed
     *
     * @param leaseId id of the lease to revoke
     * @return
     */
    @Override
    public ListenableFuture<LeaseRevokeResponse> revoke(long leaseId) {
        LeaseRevokeRequest leaseRevokeRequest =
                LeaseRevokeRequest.newBuilder()
                        .setID(leaseId)
                        .build();
        return this.leaseFutureStub.leaseRevoke(leaseRevokeRequest);
    }

    /**
     * keep alive one lease in background, this function is called in
     * user thread, it can be thread unsafe, so we sync function here
     * to avoid put twice.
     *
     * @param leaseId          id of lease to set handler
     * @param etcdLeaseHandler the handler for the lease, this value can be null
     */
    @Override
    public synchronized void keepAlive(long leaseId, EtcdLeaseHandler etcdLeaseHandler) {
        if (!this.keepAlives.containsKey(leaseId)) {
            Lease lease = new Lease(leaseId, etcdLeaseHandler);
            long now = System.currentTimeMillis();
            lease.setNextKeepAlive(now)
                    .setDeadLine(now + firstKeepAliveTimeOut);
            this.keepAlives.put(leaseId, lease);
        }
    }

    /**
     * keep alive one lease only once
     *
     * @param leaseId id of lease to keep alive once
     * @return The keep alive response
     */
    @Override
    public ListenableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId) {

        /**
         * to be completed, I will return a ListenableFuture value in the future
         */
        StreamObserver<LeaseKeepAliveRequest> requestObserver =
                this.leaseStub.leaseKeepAlive(keepAliveResponseStreamObserver);
        requestObserver.onNext(newKeepAliveRequest(leaseId));
        requestObserver.onCompleted();

        throw new UnsupportedOperationException();
    }

    /**
     * set EtcdLeaseHandler for lease
     *
     * @param leaseId          id of the lease to set handler
     * @param etcdLeaseHandler the handler for the lease
     * @throws NoSuchLeaseException if lease do not exist
     */
    @Override
    public void setEtcdLeaseHandler(long leaseId, EtcdLeaseHandler etcdLeaseHandler) throws NoSuchLeaseException {
        Lease lease = this.keepAlives.get(leaseId);
        if (lease != null) {
            /**
             * This function may be called with different threads, so
             * we sync here to do it sequentially.
             */
            synchronized (lease) {
                lease.setEtcdLeaseHandler(etcdLeaseHandler);
            }
        } else {
            throw new NoSuchLeaseException(leaseId);
        }
    }

    /**
     * Scan all the leases and send keep alive request to etcd server
     * This function is called by futureSchedule with one thread, the keepAlives
     * is instance of ConcurrentMap which allow us to do foreach with thread safe,
     * so we have no need to do extra sync.
     */
    private void keepAliveExecutor() {
        long now = System.currentTimeMillis();
        List<Long> toSendIds = new ArrayList<>();
        for (Lease l : this.keepAlives.values()) {
            if (now > l.getNextKeepAlive()) {
                toSendIds.add(l.getLeaseID());
            }
        }

        for (Long id : toSendIds) {
            this.keepAliveRequestStreamObserver.onNext(newKeepAliveRequest(id));
        }
    }

    /**
     * Scan all the leases, remove the dead leases and notify with LeaseHandler
     * This function is called by futureSchedule with one thread, the keepAlives
     * is instance of ConcurrentMap which allow us to do foreach with thread safe,
     * so we have no need to do extra sync.
     */
    private void deadLineExecutor() {
        long now = System.currentTimeMillis();
        List<Long> expireLeases = new ArrayList<>();
        for (Lease l : this.keepAlives.values()) {
            if (now > l.getDeadLine()) {
                expireLeases.add(l.getLeaseID());
            }
        }

        for (Long id : expireLeases) {
            Lease lease = this.keepAlives.get(id);
            if (lease != null && lease.isContainHandler()) {
                lease.getEtcdLeaseHandler().onLeaseExpired(id);
            }
            removeLease(id);
        }
    }

    /**
     * This method update the deadline and NextKeepAlive time
     *
     * @param leaseKeepAliveResponse The response receive from etcd server
     */
    public void processKeepAliveRespond(LeaseKeepAliveResponse leaseKeepAliveResponse) {
        long id = leaseKeepAliveResponse.getID();
        Lease lease = this.keepAlives.get(id);
        if (lease!=null) {
            /**
             * This function is called by stream callback from different thread, so
             * we sync here to make the lease set sequentially.
             */
            synchronized (lease) {
                if (leaseKeepAliveResponse.getTTL() <= 0) {
                    if (lease != null && lease.isContainHandler()) {
                        lease.getEtcdLeaseHandler().onLeaseExpired(id);
                    }
                    removeLease(id);
                } else {
                    long nextKeepAlive =
                            System.currentTimeMillis() + 1000 + leaseKeepAliveResponse.getTTL() * 1000 / 3;
                    lease.setNextKeepAlive(nextKeepAlive);
                    lease.setDeadLine(System.currentTimeMillis() + leaseKeepAliveResponse.getTTL() * 1000);
                }
            }
        }
    }

    /**
     * remove the lease from keep alive map
     *
     * @param leaseId
     */
    private void removeLease(long leaseId) {
        if (this.keepAlives.containsKey(leaseId)) {
            this.keepAlives.remove(leaseId);
        }
    }

    private LeaseKeepAliveRequest newKeepAliveRequest(long leaseId) {
        return LeaseKeepAliveRequest.newBuilder()
                .setID(leaseId)
                .build();
    }

    private void initRequestStream(StreamObserver<LeaseKeepAliveResponse> leaseKeepAliveResponseStreamObserver) {
        if (this.keepAliveRequestStreamObserver != null) {
            this.keepAliveRequestStreamObserver.onCompleted();
        }
        this.keepAliveRequestStreamObserver = this.leaseStub.leaseKeepAlive(leaseKeepAliveResponseStreamObserver);
    }


    /**
     * end the schedule for keep alive and remove dead leases
     *
     * @throws IllegalStateException if the service is not running yet
     */
    @Override
    public void closeKeepAliveService() {
        /**
         * This function is called by user thread, to make
         * thread safe, we sync here.
         */
        synchronized (this) {
            if (this.scheduledFuture != null) {
                this.keepAliveRequestStreamObserver.onCompleted();
                this.keepAliveRequestStreamObserver = null;
                this.keepAliveResponseStreamObserver = null;
                this.scheduledFuture.cancel(true);
                this.scheduledFuture = null;
            } else {
                throw new IllegalStateException("Lease keep alive service not start yet");
            }
        }
    }
}
