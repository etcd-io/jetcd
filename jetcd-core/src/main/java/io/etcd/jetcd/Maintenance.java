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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.etcd.jetcd.maintenance.*;
import io.etcd.jetcd.support.CloseableClient;

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
 *
 * <p>
 * Member-specific operations accept a host, a host and port, or an HTTP or HTTPS endpoint. The port defaults to 2379.
 * An explicit scheme must match the client's TLS configuration. Endpoints without a scheme use the client's TLS
 * settings.
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
     * Defragment one member of the cluster by its endpoint.
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
     * @param  target the etcd server endpoint.
     * @return        the response result
     */
    CompletableFuture<DefragmentResponse> defragmentMember(String target);

    /**
     * get the status of a member by its endpoint.
     *
     * @param  target the etcd server endpoint.
     * @return        the response result
     */
    CompletableFuture<StatusResponse> statusMember(String target);

    /**
     * returns a hash of the local KV state for consistency checking.
     * This is designed for testing; do not use this in production when there
     * are ongoing transactions.
     *
     * @param  target the etcd server endpoint.
     * @return        the response result
     */
    CompletableFuture<HashResponse> hash(String target);

    /**
     * returns a hash of the KV state at the time of the RPC.
     * If revision is zero, the hash is computed on all keys. If the revision
     * is non-zero, the hash is computed on all keys at or below the given revision.
     *
     * @param  target the etcd server endpoint.
     * @param  rev    the revision
     * @return        the response result
     */
    CompletableFuture<HashKVResponse> hashKV(String target, long rev);

    /**
     * Retrieves a backend snapshot. The caller owns the output stream.
     *
     * @param  output the output stream to write the snapshot content.
     * @return        the number of bytes written, once all snapshot data has been written
     */
    CompletableFuture<Long> snapshot(OutputStream output);

    /**
     * Retrieves a backend snapshot as a stream of chunks. Listener callbacks are delivered serially on worker threads.
     * The next chunk is requested after the current callback returns. If a chunk callback throws, the snapshot is canceled
     * and the listener receives the error.
     *
     * @param listener a listener for snapshot data chunks
     */
    void snapshot(Listener listener);

    /**
     * Retrieves a backend snapshot as a stream of chunks. See {@link #snapshot(Listener)} for callback delivery.
     *
     * @param onNext the on next consumer
     */
    default void snapshot(Consumer<SnapshotResponse> onNext) {
        snapshot(listener(onNext));
    }

    /**
     * Retrieves a backend snapshot as a stream of chunks. See {@link #snapshot(Listener)} for callback delivery.
     *
     * @param onNext  the on next consumer
     * @param onError the on error consumer
     */
    default void snapshot(Consumer<SnapshotResponse> onNext, Consumer<Throwable> onError) {
        snapshot(listener(onNext, onError));
    }

    /**
     * Retrieves a backend snapshot as a stream of chunks. See {@link #snapshot(Listener)} for callback delivery.
     *
     * @param onNext      the on next consumer
     * @param onError     the on error consumer
     * @param onCompleted the on completion runnable
     */
    default void snapshot(Consumer<SnapshotResponse> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        snapshot(listener(onNext, onError, onCompleted));
    }

    /**
     * moveLeader requests current leader to transfer its leadership to the transferee.
     *
     *
     * @param  transfereeID the id
     * @return              the response result
     */
    CompletableFuture<MoveLeaderResponse> moveLeader(long transfereeID);

    /**
     * Creates a snapshot listener with only onNext callback.
     *
     * @param  onNext the callback for snapshot responses
     * @return        the listener
     */
    static Listener listener(Consumer<SnapshotResponse> onNext) {
        return listener(onNext, t -> {
        }, () -> {
        });
    }

    /**
     * Creates a snapshot listener with onNext and onError callbacks.
     *
     * @param  onNext  the callback for snapshot responses
     * @param  onError the callback for errors
     * @return         the listener
     */
    static Listener listener(Consumer<SnapshotResponse> onNext, Consumer<Throwable> onError) {
        return listener(onNext, onError, () -> {
        });
    }

    /**
     * Creates a snapshot listener with all callbacks.
     *
     * @param  onNext      the callback for snapshot responses
     * @param  onError     the callback for errors
     * @param  onCompleted the callback when completed
     * @return             the listener
     */
    static Listener listener(Consumer<SnapshotResponse> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        return new Listener() {
            @Override
            public void onNext(SnapshotResponse response) {
                onNext.accept(response);
            }

            @Override
            public void onError(Throwable throwable) {
                onError.accept(throwable);
            }

            @Override
            public void onCompleted() {
                onCompleted.run();
            }
        };
    }

    /**
     * Listener for snapshot responses.
     */
    interface Listener {
        /**
         * Invoked on new snapshot responses.
         *
         * @param response the response.
         */
        void onNext(SnapshotResponse response);

        /**
         * Invoked on errors.
         *
         * @param throwable the error.
         */
        void onError(Throwable throwable);

        /**
         * Invoked on completion.
         */
        void onCompleted();
    }
}
