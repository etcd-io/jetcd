/**
 * Copyright 2017 The jetcd authors
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

package io.etcd.jetcd.internal.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.api.AlarmRequest;
import io.etcd.jetcd.api.AlarmType;
import io.etcd.jetcd.api.DefragmentRequest;
import io.etcd.jetcd.api.HashKVRequest;
import io.etcd.jetcd.api.MaintenanceGrpc;
import io.etcd.jetcd.api.MoveLeaderRequest;
import io.etcd.jetcd.api.SnapshotRequest;
import io.etcd.jetcd.api.SnapshotResponse;
import io.etcd.jetcd.api.StatusRequest;
import io.etcd.jetcd.maintenance.AlarmResponse;
import io.etcd.jetcd.maintenance.DefragmentResponse;
import io.etcd.jetcd.maintenance.HashKVResponse;
import io.etcd.jetcd.maintenance.MoveLeaderResponse;
import io.etcd.jetcd.maintenance.StatusResponse;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of maintenance client.
 */
class MaintenanceImpl implements Maintenance {

  private final ClientConnectionManager connectionManager;
  private final MaintenanceGrpc.MaintenanceFutureStub stub;
  private final MaintenanceGrpc.MaintenanceStub streamStub;

  MaintenanceImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(MaintenanceGrpc::newFutureStub);
    this.streamStub = connectionManager.newStub(MaintenanceGrpc::newStub);
  }

  /**
   * get all active keyspace alarm.
   *
   * @return alarm list
   */
  @Override
  public CompletableFuture<AlarmResponse> listAlarms() {
    AlarmRequest alarmRequest = AlarmRequest.newBuilder()
        .setAlarm(AlarmType.NONE)
        .setAction(AlarmRequest.AlarmAction.GET)
        .setMemberID(0).build();

    return Util.toCompletableFuture(
        this.stub.alarm(alarmRequest),
        AlarmResponse::new,
        this.connectionManager.getExecutorService()
    );
  }

  /**
   * disarms a given alarm.
   *
   * @param member the alarm
   * @return the response result
   */
  @Override
  public CompletableFuture<AlarmResponse> alarmDisarm(
      io.etcd.jetcd.maintenance.AlarmMember member) {
    checkArgument(member.getMemberId() != 0, "the member id can not be 0");
    checkArgument(member.getAlarmType() != io.etcd.jetcd.maintenance.AlarmType.NONE,
        "alarm type can not be NONE");

    AlarmRequest alarmRequest = AlarmRequest.newBuilder()
        .setAlarm(AlarmType.NOSPACE)
        .setAction(AlarmRequest.AlarmAction.DEACTIVATE)
        .setMemberID(member.getMemberId())
        .build();

    return Util.toCompletableFuture(
        this.stub.alarm(alarmRequest),
        AlarmResponse::new,
        this.connectionManager.getExecutorService()
    );
  }

  /**
   * defragment one member of the cluster.
   *
   * <p>After compacting the keyspace, the backend database may exhibit internal
   * fragmentation. Any internal fragmentation is space that is free to use
   * by the backend but still consumes storage space. The process of
   * defragmentation releases this storage space back to the file system.
   * Defragmentation is issued on a per-member so that cluster-wide latency
   * spikes may be avoided.
   *
   * <p>Defragment is an expensive operation. User should avoid defragmenting
   * multiple members at the same time.
   * To defragment multiple members in the cluster, user need to call defragment
   * multiple times with different endpoints.
   */
  @Override
  public CompletableFuture<DefragmentResponse> defragmentMember(
      String endpoint) {
    return this.connectionManager.withNewChannel(
        endpoint,
        MaintenanceGrpc::newFutureStub,
        stub -> Util.toCompletableFuture(
            stub.defragment(DefragmentRequest.getDefaultInstance()),
            DefragmentResponse::new,
            this.connectionManager.getExecutorService()
        )
    );
  }

  /**
   * get the status of one member.
   */
  @Override
  public CompletableFuture<StatusResponse> statusMember(
      String endpoint) {
    return this.connectionManager.withNewChannel(
        endpoint,
        MaintenanceGrpc::newFutureStub,
        stub -> Util.toCompletableFuture(
            stub.status(StatusRequest.getDefaultInstance()),
            StatusResponse::new,
            this.connectionManager.getExecutorService()
        )
    );
  }

  @Override
  public CompletableFuture<MoveLeaderResponse> moveLeader(long transfereeID) {
    return Util.toCompletableFuture(
      this.stub.moveLeader(
        MoveLeaderRequest.newBuilder()
          .setTargetID(transfereeID)
          .build()
      ),
      MoveLeaderResponse::new,
      this.connectionManager.getExecutorService()
    );
  }

  @Override
  public CompletableFuture<HashKVResponse> hashKV(String endpoint, long rev) {
    return this.connectionManager.withNewChannel(
        endpoint,
        MaintenanceGrpc::newFutureStub,
        stub -> Util.toCompletableFuture(
            stub.hashKV(HashKVRequest.newBuilder().setRevision(rev).build()),
            HashKVResponse::new,
            this.connectionManager.getExecutorService()
        )
    );
  }

  @Override
  public CompletableFuture<Long> snapshot(OutputStream outputStream) {
    final CompletableFuture<Long> answer = new CompletableFuture<>();
    final AtomicLong bytes = new AtomicLong(0);

    this.streamStub.snapshot(SnapshotRequest.getDefaultInstance(),  new StreamObserver<SnapshotResponse>() {
      @Override
      public void onNext(SnapshotResponse snapshotResponse) {
        try {
          snapshotResponse.getBlob().writeTo(outputStream);

          bytes.addAndGet(snapshotResponse.getBlob().size());
        } catch (IOException e) {
          answer.completeExceptionally(toEtcdException(e));
        }
      }

      @Override
      public void onError(Throwable throwable) {
        answer.completeExceptionally(toEtcdException(throwable));
      }

      @Override
      public void onCompleted() {
        answer.complete(bytes.get());
      }
    });

    return answer;
  }

  @Override
  public void snapshot(StreamObserver<io.etcd.jetcd.maintenance.SnapshotResponse> observer) {
    this.streamStub.snapshot(SnapshotRequest.getDefaultInstance(),  new StreamObserver<SnapshotResponse>() {
      @Override
      public void onNext(SnapshotResponse snapshotResponse) {
        observer.onNext(new io.etcd.jetcd.maintenance.SnapshotResponse(snapshotResponse));
      }

      @Override
      public void onError(Throwable throwable) {
        observer.onError(toEtcdException(throwable));
      }

      @Override
      public void onCompleted() {
        observer.onCompleted();
      }
    });
  }
}
