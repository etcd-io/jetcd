package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.data.EtcdHeader;
import com.coreos.jetcd.lease.Lease;
import com.coreos.jetcd.lease.NoSuchLeaseException;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static com.coreos.jetcd.EtcdUtil.*;

/**
 * Implementation of lease client
 */
public class EtcdLeaseImpl implements EtcdLease {

    private final static int                       DEFAULT_TTL           = 5000;
    private final static int                       DEFAULT_SCAN_PERIOD   = 500;
    private final LeaseGrpc.LeaseFutureStub        leaseFutureStub;
    private final LeaseGrpc.LeaseStub              leaseStub;
    /**
     * gRPC channel
     */
    private ManagedChannel                         channel;
    /**
     * Timer schedule to send keep alive request
     */
    private ScheduledExecutorService               keepAliveSchedule;
    private ScheduledFuture<?>                     scheduledFuture;
    private Supplier<Executor>                     callExecutor;
    private long                                   scanPeriod;

    private final Map<Long, LeaseHolder>                 keepAlives            = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<EtcdHeader>>    onceKeepAlives = new ConcurrentHashMap<>();

    /**
     * The first time interval
     */
    private long                                   firstKeepAliveTimeOut = DEFAULT_TTL;

    /**
     * KeepAlive Request Stream, put request into this stream to keep the lease alive
     */
    private StreamObserver<LeaseKeepAliveRequest>  keepAliveRequestStreamObserver;

    /**
     * KeepAlive Response Streamer, receive keep alive response from this stream and update the
     * nextKeepAliveTime and deadline of the leases.
     */
    private StreamObserver<LeaseKeepAliveResponse> keepAliveResponseStreamObserver;

    public EtcdLeaseImpl(final ManagedChannel channel, Optional<String> token) {
        /**
         * Init lease stub
         */
        this.channel = channel;
        this.leaseFutureStub = EtcdClientUtil.configureStub(LeaseGrpc.newFutureStub(this.channel), token);
        this.leaseStub = EtcdClientUtil.configureStub(LeaseGrpc.newStub(this.channel), token);
        this.scanPeriod = DEFAULT_SCAN_PERIOD;
        callExecutor = Suppliers.memoize(()->Executors.newSingleThreadExecutor());
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
                throw new IllegalStateException("Lease keep alive service already started");
            }
            keepAliveResponseStreamObserver = new StreamObserver<LeaseKeepAliveResponse>() {
                @Override
                public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
                    processKeepAliveRespond(leaseKeepAliveResponse);
                    LeaseHolder leaseHolder = keepAlives.get(leaseKeepAliveResponse.getID());
                    if (leaseHolder != null && leaseHolder.isContainHandler()) {
                        leaseHolder.getEtcdLeaseHandler().onKeepAliveRespond(leaseKeepAliveResponse);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                }
            };

            initRequestStream(keepAliveResponseStreamObserver);

            /**
             * Start heartbeat schedule to keep alive leases and remove dead leases
             */
            if (this.keepAliveSchedule == null) {
                this.keepAliveSchedule = Executors.newSingleThreadScheduledExecutor();
            }
            this.scheduledFuture = this.keepAliveSchedule.scheduleAtFixedRate(() -> {
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
    public CompletableFuture<Lease> grant(long ttl) {
        LeaseGrantRequest leaseGrantRequest = LeaseGrantRequest.newBuilder().setTTL(ttl).build();
        return completableFromListenableFuture(this.leaseFutureStub.leaseGrant(leaseGrantRequest),
                (LeaseGrantResponse l)->apiToClientLease(l),
                callExecutor.get());
    }

    /**
     * revoke one lease and the key bind to this lease will be removed
     *
     * @param lease the lease to revoke
     * @return
     */
    @Override
    public CompletableFuture<EtcdHeader> revoke(Lease lease) {
        LeaseRevokeRequest leaseRevokeRequest = LeaseRevokeRequest.newBuilder().setID(lease.getLeaseID()).build();
        return completableFromListenableFuture(this.leaseFutureStub.leaseRevoke(leaseRevokeRequest),
                (LeaseRevokeResponse l)->apiToClientHeader(l.getHeader()),
                callExecutor.get());
    }

    /**
     * keep alive one lease in background, this function is called in
     * user thread, it can be thread unsafe, so we sync function here
     * to avoid put twice.
     *
     * @param lease          of lease to set handler
     * @param etcdLeaseHandler the handler for the lease, this value can be null
     *
     * @throws IllegalStateException this exception hints that the keep alive service wasn't started
     */
    @Override
    public synchronized void keepAlive(Lease lease, EtcdLeaseHandler etcdLeaseHandler) {
        if(!isKeepAliveServiceRunning()){
            throw new IllegalStateException("Lease keep alive service not started yet");
        }
        if (!this.keepAlives.containsKey(lease.getLeaseID())) {
            LeaseHolder leaseHolder = new LeaseHolder(lease, etcdLeaseHandler);
            long now = System.currentTimeMillis();
            leaseHolder.setNextKeepAlive(now).setDeadLine(now + firstKeepAliveTimeOut);
            this.keepAlives.put(lease.getLeaseID(), leaseHolder);
        }
    }

    /**
     * cancel keep alive for lease in background
     *
     * @param lease lease to cancel keep alive
     */
    @Override
    public synchronized void cancelKeepAlive(Lease lease) throws ExecutionException, InterruptedException {
        if(!isKeepAliveServiceRunning()){
            throw new IllegalStateException("Lease keep alive service not started yet");
        }
        if (this.keepAlives.containsKey(lease.getLeaseID())) {
            keepAlives.remove(lease.getLeaseID());
            revoke(lease).get();
        }else{
            throw new IllegalStateException("Lease is not registered in the keep alive service");
        }
    }

    /**
     * keep alive one lease only once
     *
     * @param lease lease to keep alive once
     * @return The keep alive response
     */
    @Override
    public synchronized CompletableFuture<EtcdHeader> keepAliveOnce(Lease lease) {
        if(keepAlives.containsKey(lease.getLeaseID())||onceKeepAlives.containsKey(lease.getLeaseID())){
            throw new IllegalStateException("lease already registered in keep alive service");
        }
        if(!isKeepAliveServiceRunning()){
            throw new IllegalStateException("Lease keep alive service not started yet");
        }
        CompletableFuture<EtcdHeader> completableFuture = new CompletableFuture<>();
        onceKeepAlives.put(lease.getLeaseID(), completableFuture);
        this.keepAliveRequestStreamObserver.onNext(newKeepAliveRequest(lease.getLeaseID()));
        return completableFuture;
    }

    /**
     * set EtcdLeaseHandler for lease
     *
     * @param lease          lease to set handler
     * @param etcdLeaseHandler the handler for the lease
     * @throws NoSuchLeaseException if lease do not exist
     */
    @Override
    public void setEtcdLeaseHandler(Lease lease, EtcdLeaseHandler etcdLeaseHandler) throws NoSuchLeaseException {
        LeaseHolder leaseHolder = this.keepAlives.get(lease.getLeaseID());
        if (leaseHolder != null) {
            /**
             * This function may be called with different threads, so
             * we sync here to do it sequentially.
             */
            synchronized (leaseHolder) {
                leaseHolder.setEtcdLeaseHandler(etcdLeaseHandler);
            }
        } else {
            throw new NoSuchLeaseException(lease.getLeaseID());
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
        for (LeaseHolder l : this.keepAlives.values()) {
            if (now > l.getNextKeepAlive()) {
                toSendIds.add(l.lease.getLeaseID());
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
        for (LeaseHolder l : this.keepAlives.values()) {
            if (now > l.getDeadLine()) {
                expireLeases.add(l.lease.getLeaseID());
            }
        }

        for (Long id : expireLeases) {
            LeaseHolder lease = this.keepAlives.get(id);
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
        LeaseHolder leaseHolder = this.keepAlives.get(id);
        if (leaseHolder != null) {
            /**
             * This function is called by stream callback from different thread, so
             * we sync here to make the lease set sequentially.
             */
            synchronized (leaseHolder) {
                if (leaseKeepAliveResponse.getTTL() <= 0) {
                    if (leaseHolder != null && leaseHolder.isContainHandler()) {
                        leaseHolder.getEtcdLeaseHandler().onLeaseExpired(id);
                    }
                    removeLease(id);
                } else {
                    long nextKeepAlive = System.currentTimeMillis() + 1000 + leaseKeepAliveResponse.getTTL() * 1000 / 3;
                    leaseHolder.setNextKeepAlive(nextKeepAlive);
                    leaseHolder.setDeadLine(System.currentTimeMillis() + leaseKeepAliveResponse.getTTL() * 1000);
                }
            }
        }else if(onceKeepAlives.containsKey(id)){
            onceKeepAlives.remove(id).complete(apiToClientHeader(leaseKeepAliveResponse.getHeader()));
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
        return LeaseKeepAliveRequest.newBuilder().setID(leaseId).build();
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
                throw new IllegalStateException("Lease keep alive service not started yet");
            }
        }
    }

    static class LeaseHolder{

        final Lease lease;

        private LeaseGrantResponse         leaseGrantResponse;

        private long                       deadLine;

        private long                       nextKeepAlive;

        private EtcdLease.EtcdLeaseHandler etcdLeaseHandler;

        public LeaseHolder(Lease lease) {
            this.lease = lease;
        }

        public LeaseHolder(Lease lease, EtcdLeaseHandler etcdLeaseHandler) {
            this.lease = lease;
            this.etcdLeaseHandler = etcdLeaseHandler;
        }

        public long getDeadLine() {
            return deadLine;
        }

        LeaseHolder setDeadLine(long deadLine) {
            this.deadLine = deadLine;
            return this;
        }

        public long getNextKeepAlive() {
            return nextKeepAlive;
        }

        public LeaseHolder setNextKeepAlive(long nextKeepAlive) {
            this.nextKeepAlive = nextKeepAlive;
            return this;
        }

        public boolean isContainHandler() {
            return etcdLeaseHandler != null;
        }

        public EtcdLease.EtcdLeaseHandler getEtcdLeaseHandler() {
            return etcdLeaseHandler;
        }

        public void setEtcdLeaseHandler(EtcdLease.EtcdLeaseHandler etcdLeaseHandler) {
            this.etcdLeaseHandler = etcdLeaseHandler;
        }
    }


}
