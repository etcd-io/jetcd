package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.lease.Lease;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.security.InvalidParameterException;
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
        this(channel, DEFAULT_SCAN_PERIOD);
    }

    public EtcdLeaseImpl(final ManagedChannel channel, long scanPeriod) {

        /*
        Init lease stub
         */
        this.channel = channel;
        this.leaseFutureStub = LeaseGrpc.newFutureStub(this.channel);
        this.leaseStub = LeaseGrpc.newStub(this.channel);
        this.scanPeriod = scanPeriod;
    }

    /**
     * Init the request stream to etcd
     * start schedule to keep heartbeat to keep alive and remove dead leases
     */
    public void startKeepAliveService() {
        if(scheduledFuture!=null){
            throw new IllegalStateException("Lease keep alive service already start");
        }
        keepAliveResponseStreamObserver = new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
                processKeepAliveRespond(leaseKeepAliveResponse);
                Lease lease = keepAlives.get(leaseKeepAliveResponse.getID());
                if (lease!=null&&lease.isContainHandler()) {
                    lease.getEtcdLeaseHandler().onKeepAliveRespond(leaseKeepAliveResponse);
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {}
        };

        initRequestStream(keepAliveResponseStreamObserver);

        /**
         * Start heartbeat schedule to keep alive leases and remove dead leases
         */
        if(keepAliveSchedule==null){
            keepAliveSchedule = Executors.newSingleThreadScheduledExecutor();
        }
        scheduledFuture = keepAliveSchedule.scheduleAtFixedRate(
                ()-> {
                    keepAliveExecutor();
                    deadLineExecutor();
                }, 0, scanPeriod, TimeUnit.MILLISECONDS);
    }

    /**
     * New a lease with ttl value
     * @param ttl ttl value
     * @return The response from etcd
     */
    @Override
    public ListenableFuture<LeaseGrantResponse> grant(long ttl) {
        LeaseGrantRequest leaseGrantRequest =
                LeaseGrantRequest.newBuilder()
                        .setTTL(ttl)
                        .build();
        return leaseFutureStub.leaseGrant(leaseGrantRequest);
    }

    /**
     * revoke one lease and the key bind to this lease will be removed
     * @param leaseId the id of lease
     * @return
     */
    @Override
    public ListenableFuture<LeaseRevokeResponse> revoke(long leaseId) {
        LeaseRevokeRequest leaseRevokeRequest =
                LeaseRevokeRequest.newBuilder()
                        .setID(leaseId)
                        .build();
        return leaseFutureStub.leaseRevoke(leaseRevokeRequest);
    }

    /**
     * keep alive one lease in background
     *
     * @param leaseId          id of lease to set handler
     * @param etcdLeaseHandler the handler for the lease, this value can be null
     */
    @Override
    public void keepAlive(long leaseId, EtcdLeaseHandler etcdLeaseHandler) {
        if (!keepAlives.containsKey(leaseId)) {
            Lease lease = new Lease(leaseId, etcdLeaseHandler);
            long now = System.currentTimeMillis();
            lease.setNextKeepAlive(now)
                    .setDeadLine(now + firstKeepAliveTimeOut);
            keepAlives.put(leaseId, lease);
        }
    }

    /**
     * keep alive one lease only once
     * @param leaseId
     * @return The keepAlive response
     */
    @Override
    public ListenableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId) {

        /**
         * to be completed, I will return a ListenableFuture value in the future
         */
        StreamObserver<LeaseKeepAliveRequest> requestObserver =
                leaseStub.leaseKeepAlive(keepAliveResponseStreamObserver);
        requestObserver.onNext(newKeepAliveRequest(leaseId));
        requestObserver.onCompleted();

        throw new UnsupportedOperationException();
    }

     /**
     * set EtcdLeaseHandler for lease
     *
     * @param leaseId          id of the lease to set handler
     * @param etcdLeaseHandler the handler for the lease
     * @throws InvalidParameterException if lease do not exist
     */
    @Override
    public void setEtcdLeaseHandler(long leaseId, EtcdLeaseHandler etcdLeaseHandler) {
        Lease lease = keepAlives.get(leaseId);
        if(lease!=null){
            lease.setEtcdLeaseHandler(etcdLeaseHandler);
        }else{
            throw new InvalidParameterException("There is no lease: " + lease.getLeaseID());
        }
    }

    /**
     * Scan all the leases and send keep alive request to etcd server
     */
    private void keepAliveExecutor(){
            long now = System.currentTimeMillis();
            List<Long> toSendIds = new ArrayList<>();
            for(Lease l : keepAlives.values()){
                if(now > l.getNextKeepAlive()){
                    toSendIds.add(l.getLeaseID());
                }
            }

            for(Long id: toSendIds){
                keepAliveRequestStreamObserver.onNext(newKeepAliveRequest(id));
            }
    }

    /**
     * Scan all the leases, remove the dead leases and notify with LeaseHandler
     */
    private void deadLineExecutor() {
        long now = System.currentTimeMillis();
        List<Long> expireLeases = new ArrayList<>();
        for (Lease l : keepAlives.values()) {
            if (now > l.getDeadLine()) {
                expireLeases.add(l.getLeaseID());
            }
        }

        for (Long id : expireLeases) {
            Lease lease = keepAlives.get(id);
            if (lease != null && lease.isContainHandler()) {
                lease.getEtcdLeaseHandler().onLeaseExpired(id);
            }
            removeLease(id);
        }
    }

    /**
     * This method update the deadline and NextKeepAlive time
     * @param leaseKeepAliveResponse The response receive from etcd server
     */
    public void processKeepAliveRespond(LeaseKeepAliveResponse leaseKeepAliveResponse) {
        if (keepAlives.containsKey(leaseKeepAliveResponse.getID())) {
            long id = leaseKeepAliveResponse.getID();
            Lease lease = keepAlives.get(id);
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

    /**
     * remove the lease from keep alive map
     * @param leaseId
     */
    private void removeLease(long leaseId){
            if(keepAlives.containsKey(leaseId)){
                keepAlives.remove(leaseId);
            }
    }

    private LeaseKeepAliveRequest newKeepAliveRequest(long leaseId){
        return LeaseKeepAliveRequest.newBuilder()
                        .setID(leaseId)
                        .build();
    }

    private void initRequestStream(StreamObserver<LeaseKeepAliveResponse> leaseKeepAliveResponseStreamObserver) {
        if (keepAliveRequestStreamObserver != null) {
            keepAliveRequestStreamObserver.onCompleted();
            keepAliveRequestStreamObserver = null;
        }
        keepAliveRequestStreamObserver = leaseStub.leaseKeepAlive(leaseKeepAliveResponseStreamObserver);
    }


    /**
     * end the background heartbeat service for keep alive
     */
    @Override
    public void closeKeepAliveService() {
        if (scheduledFuture != null) {
            keepAliveRequestStreamObserver.onCompleted();
            keepAliveRequestStreamObserver = null;
            keepAliveResponseStreamObserver = null;
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }else{
            throw new IllegalStateException("Lease keep alive service not start yet");
        }
    }


}
