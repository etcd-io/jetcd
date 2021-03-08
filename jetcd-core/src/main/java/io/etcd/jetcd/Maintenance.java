/*
 * Copyright 2016-2021 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd;

import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import io.etcd.jetcd.maintenance.AlarmMember;
import io.etcd.jetcd.maintenance.AlarmResponse;
import io.etcd.jetcd.maintenance.DefragmentResponse;
import io.etcd.jetcd.maintenance.HashKVResponse;
import io.etcd.jetcd.maintenance.MoveLeaderResponse;
import io.etcd.jetcd.maintenance.SnapshotResponse;
import io.etcd.jetcd.maintenance.StatusResponse;
import io.etcd.jetcd.support.CloseableClient;
import io.grpc.stub.StreamObserver;

/**
 * Interface of maintenance talking to etcd.
 *
 * <p>
 * An etcd cluster needs periodic maintenance to remain reliable. Depending
 * on an etcd application's needs, this maintenance can usually be
 * automated and performed without downtime or significantly degraded
 * performance.
 *
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
public interface Maintenance extends CloseableClient {

    /**
     * get all active keyspace alarm.
     * 
     * @return the response
     */
    CompletableFuture<AlarmResponse> listAlarms();

    /**
     * disarms a given alarm.
     *
     * @param  member the alarm
     * @return        the response result
     */
    CompletableFuture<AlarmResponse> alarmDisarm(AlarmMember member);

    /**
     * defragment one member of the cluster by its endpoint.
     *
     * <p>
     * After compacting the keyspace, the backend database may exhibit internal
     * fragmentation. Any internal fragmentation is space that is free to use
     * by the backend but still consumes storage space. The process of
     * defragmentation releases this storage space back to the file system.
     * Defragmentation is issued on a per-member so that cluster-wide latency
     * spikes may be avoided.
     *
     * <p>
     * Defragment is an expensive operation. User should avoid defragmenting
     * multiple members at the same time.
     * To defragment multiple members in the cluster, user need to call defragment
     * multiple times with different endpoints.
     *
     * @param  endpoint the etcd server endpoint.
     * @return          the response result
     */
    CompletableFuture<DefragmentResponse> defragmentMember(URI endpoint);

    /**
     * get the status of a member by its endpoint.
     *
     * @param  endpoint the etcd server endpoint.
     * @return          the response result
     */
    CompletableFuture<StatusResponse> statusMember(URI endpoint);

    /**
     * returns a hash of the KV state at the time of the RPC.
     * If revision is zero, the hash is computed on all keys. If the revision
     * is non-zero, the hash is computed on all keys at or below the given revision.
     *
     * @param  endpoint the etcd server endpoint.
     * @param  rev      the revision
     * @return          the response result
     */
    CompletableFuture<HashKVResponse> hashKV(URI endpoint, long rev);

    /**
     * retrieves backend snapshot.
     *
     * @param  output the output stream to write the snapshot content.
     * @return        a Snapshot for retrieving backend snapshot.
     */
    CompletableFuture<Long> snapshot(OutputStream output);

    /**
     * retrieves backend snapshot as as stream of chunks.
     *
     * @param observer a stream of data chunks
     */
    void snapshot(StreamObserver<SnapshotResponse> observer);

    /**
     * moveLeader requests current leader to transfer its leadership to the transferee.
     *
     *
     * @param  transfereeID the id
     * @return              the response result
     */
    CompletableFuture<MoveLeaderResponse> moveLeader(long transfereeID);
}
