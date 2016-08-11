package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of maintenance client
 */
public class EtcdMaintenanceImpl implements EtcdMaintenance {

    private MaintenanceGrpc.MaintenanceFutureStub futureStub;
    private MaintenanceGrpc.MaintenanceStub streamStub;
    private volatile StreamObserver<SnapshotResponse> snapshotObserver;
    private volatile SnapshotCallback snapshotCallback;

    public EtcdMaintenanceImpl(MaintenanceGrpc.MaintenanceFutureStub futureStub,
                               MaintenanceGrpc.MaintenanceStub streamStub) {
        this.futureStub = futureStub;
        this.streamStub = streamStub;
    }

    /**
     * get all active keyspace alarm
     *
     * @return alarm list
     */
    @Override
    public ListenableFuture<AlarmResponse> listAlarms() {
        AlarmRequest alarmRequest = AlarmRequest.newBuilder()
                .setAlarm(AlarmType.NONE)
                .setAction(AlarmRequest.AlarmAction.GET)
                .setMemberID(0).build();
        return this.futureStub.alarm(alarmRequest);
    }

    /**
     * disarms a given alarm
     *
     * @param member the alarm
     * @return the response result
     */
    @Override
    public ListenableFuture<AlarmResponse> disalarm(AlarmMember member) {
        AlarmRequest alarmRequest = AlarmRequest.newBuilder()
                .setAlarm(AlarmType.NOSPACE)
                .setAction(AlarmRequest.AlarmAction.DEACTIVATE)
                .setMemberID(member.getMemberID())
                .build();
        checkArgument(member.getMemberID() != 0, "the member id can not be 0");
        checkArgument(member.getAlarm() != AlarmType.NONE, "alarm type can not be NONE");
        return this.futureStub.alarm(alarmRequest);
    }

    /**
     * defragment one member of the cluster
     * <p>
     * After compacting the keyspace, the backend database may exhibit internal
     * fragmentation. Any internal fragmentation is space that is free to use
     * by the backend but still consumes storage space. The process of
     * defragmentation releases this storage space back to the file system.
     * Defragmentation is issued on a per-member so that cluster-wide latency
     * spikes may be avoided.
     * <p>
     * Defragment is an expensive operation. User should avoid defragmenting
     * multiple members at the same time.
     * To defragment multiple members in the cluster, user need to call defragment
     * multiple times with different endpoints.
     */
    @Override
    public ListenableFuture<DefragmentResponse> defragmentMember() {
        return this.futureStub.defragment(DefragmentRequest.getDefaultInstance());
    }

    /**
     * get the status of one member
     */
    @Override
    public ListenableFuture<StatusResponse> statusMember() {
        return this.futureStub.status(StatusRequest.getDefaultInstance());
    }

    /**
     * Set callback for snapshot
     * <p> The onSnapshot will be called when the member make a snapshot.
     * <p> The onError will be called as exception, and the callback will be canceled.
     *
     * @param callback Snapshot callback
     */
    @Override
    public synchronized void setSnapshotCallback(SnapshotCallback callback) {
        if (this.snapshotObserver == null) {
            this.snapshotObserver = new StreamObserver<SnapshotResponse>() {
                @Override
                public void onNext(SnapshotResponse snapshotResponse) {
                    if (snapshotCallback != null) {
                        synchronized (EtcdMaintenanceImpl.this) {
                            if (snapshotCallback != null) {
                                snapshotCallback.onSnapShot(snapshotResponse);
                            }
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    synchronized (EtcdMaintenanceImpl.this) {
                        if (snapshotCallback != null) {
                            snapshotCallback.onError(throwable);
                        }
                        snapshotObserver = null;
                    }
                }

                @Override
                public void onCompleted() {

                }
            };
        }

        this.streamStub.snapshot(SnapshotRequest.getDefaultInstance(), this.snapshotObserver);
    }

    /**
     * Remove callback for snapshot.
     */
    @Override
    public synchronized void removeSnapShotCallback() {
        if(this.snapshotObserver != null){
            snapshotObserver.onCompleted();
            snapshotCallback = null;
            snapshotObserver = null;
        }
    }
}
