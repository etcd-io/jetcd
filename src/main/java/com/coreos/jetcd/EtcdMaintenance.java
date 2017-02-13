package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;

/**
 * Interface of maintenance talking to etcd
 * <p>
 * An etcd cluster needs periodic maintenance to remain reliable. Depending
 * on an etcd application's needs, this maintenance can usually be
 * automated and performed without downtime or significantly degraded
 * performance.
 * <p>
 * All etcd maintenance manages storage resources consumed by the etcd
 * keyspace. Failure to adequately control the keyspace size is guarded by
 * storage space quotas; if an etcd member runs low on space, a quota will
 * trigger cluster-wide alarms which will put the system into a
 * limited-operation maintenance mode. To avoid running out of space for
 * writes to the keyspace, the etcd keyspace history must be compacted.
 * Storage space itself may be reclaimed by defragmenting etcd members.
 * Finally, periodic snapshot backups of etcd member state makes it possible
 * to recover any unintended logical data loss or corruption caused by
 * operational error.
 */
public interface EtcdMaintenance {

  /**
   * get all active keyspace alarm
   */
  ListenableFuture<AlarmResponse> listAlarms();

  /**
   * disarms a given alarm
   *
   * @param member the alarm
   * @return the response result
   */
  ListenableFuture<AlarmResponse> disalarm(AlarmMember member);

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
  ListenableFuture<DefragmentResponse> defragmentMember();

  /**
   * get the status of one member
   */
  ListenableFuture<StatusResponse> statusMember();

  /**
   * Set callback for snapshot.
   * <p> The onSnapshot will be called when the member make a snapshot.
   * <p> The onError will be called as exception, and the callback will be canceled.
   *
   * @param callback Snapshot callback
   */
  void setSnapshotCallback(SnapshotCallback callback);

  /**
   * Remove callback for snapshot.
   */
  void removeSnapShotCallback();

  /**
   * Callback to process snapshot events.
   */
  interface SnapshotCallback {

    /**
     * The onSnapshot will be called when the member make a snapshot
     *
     * @param snapshotResponse snapshot response
     */
    void onSnapShot(SnapshotResponse snapshotResponse);

    /**
     * The onError will be called as exception, and the callback will be canceled.
     */
    void onError(Throwable throwable);
  }
}
