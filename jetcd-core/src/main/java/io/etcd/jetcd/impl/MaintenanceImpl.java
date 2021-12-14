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

package io.etcd.jetcd.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.api.AlarmRequest;
import io.etcd.jetcd.api.AlarmType;
import io.etcd.jetcd.api.DefragmentRequest;
import io.etcd.jetcd.api.HashKVRequest;
import io.etcd.jetcd.api.MaintenanceGrpc;
import io.etcd.jetcd.api.MoveLeaderRequest;
import io.etcd.jetcd.api.SnapshotRequest;
import io.etcd.jetcd.api.StatusRequest;
import io.etcd.jetcd.api.VertxMaintenanceGrpc;
import io.etcd.jetcd.maintenance.AlarmResponse;
import io.etcd.jetcd.maintenance.DefragmentResponse;
import io.etcd.jetcd.maintenance.HashKVResponse;
import io.etcd.jetcd.maintenance.MoveLeaderResponse;
import io.etcd.jetcd.maintenance.StatusResponse;
import io.etcd.jetcd.support.Util;
import io.grpc.stub.StreamObserver;

import static com.google.common.base.Preconditions.checkArgument;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

/**
 * Implementation of maintenance client.
 */
final class MaintenanceImpl extends Impl implements Maintenance {
    private final VertxMaintenanceGrpc.MaintenanceVertxStub stub;

    MaintenanceImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager().newStub(VertxMaintenanceGrpc::newVertxStub);
    }

    @Override
    public CompletableFuture<AlarmResponse> listAlarms() {
        AlarmRequest alarmRequest = AlarmRequest.newBuilder()
            .setAlarm(AlarmType.NONE)
            .setAction(AlarmRequest.AlarmAction.GET)
            .setMemberID(0)
            .build();

        return completable(this.stub.alarm(alarmRequest), AlarmResponse::new);
    }

    @Override
    public CompletableFuture<AlarmResponse> alarmDisarm(io.etcd.jetcd.maintenance.AlarmMember member) {
        checkArgument(member.getMemberId() != 0, "the member id can not be 0");
        checkArgument(member.getAlarmType() != io.etcd.jetcd.maintenance.AlarmType.NONE, "alarm type can not be NONE");

        AlarmRequest alarmRequest = AlarmRequest.newBuilder()
            .setAlarm(AlarmType.NOSPACE)
            .setAction(AlarmRequest.AlarmAction.DEACTIVATE)
            .setMemberID(member.getMemberId())
            .build();

        return completable(this.stub.alarm(alarmRequest), AlarmResponse::new);
    }

    @Override
    public CompletableFuture<DefragmentResponse> defragmentMember(URI endpoint) {
        return this.connectionManager().withNewChannel(
            // TODO
            endpoint.toString(),
            MaintenanceGrpc::newFutureStub,
            stub -> Util.toCompletableFuture(
                stub.defragment(DefragmentRequest.getDefaultInstance()),
                DefragmentResponse::new,
                this.connectionManager().getExecutorService()));
    }

    @Override
    public CompletableFuture<DefragmentResponse> defragmentMember(String target) {
        return this.connectionManager().withNewChannel(
            target,
            MaintenanceGrpc::newFutureStub,
            stub -> Util.toCompletableFuture(
                stub.defragment(DefragmentRequest.getDefaultInstance()),
                DefragmentResponse::new,
                this.connectionManager().getExecutorService()));
    }

    @Override
    public CompletableFuture<StatusResponse> statusMember(URI endpoint) {
        return this.connectionManager().withNewChannel(
            // TODO
            endpoint.toString(),
            MaintenanceGrpc::newFutureStub,
            stub -> Util.toCompletableFuture(
                stub.status(StatusRequest.getDefaultInstance()),
                StatusResponse::new,
                this.connectionManager().getExecutorService()));
    }

    @Override
    public CompletableFuture<StatusResponse> statusMember(String target) {
        return this.connectionManager().withNewChannel(
            target,
            MaintenanceGrpc::newFutureStub,
            stub -> Util.toCompletableFuture(
                stub.status(StatusRequest.getDefaultInstance()),
                StatusResponse::new,
                this.connectionManager().getExecutorService()));
    }

    @Override
    public CompletableFuture<MoveLeaderResponse> moveLeader(long transfereeID) {
        return completable(
            this.stub.moveLeader(MoveLeaderRequest.newBuilder().setTargetID(transfereeID).build()),
            MoveLeaderResponse::new);
    }

    @Override
    public CompletableFuture<HashKVResponse> hashKV(URI endpoint, long rev) {
        return this.connectionManager().withNewChannel(
            // TODO
            endpoint.toString(),
            MaintenanceGrpc::newFutureStub,
            stub -> Util.toCompletableFuture(
                stub.hashKV(HashKVRequest.newBuilder().setRevision(rev).build()),
                HashKVResponse::new,
                this.connectionManager().getExecutorService()));
    }

    @Override
    public CompletableFuture<HashKVResponse> hashKV(String target, long rev) {
        return this.connectionManager().withNewChannel(
            target,
            MaintenanceGrpc::newFutureStub,
            stub -> Util.toCompletableFuture(
                stub.hashKV(HashKVRequest.newBuilder().setRevision(rev).build()),
                HashKVResponse::new,
                this.connectionManager().getExecutorService()));
    }

    @Override
    public CompletableFuture<Long> snapshot(OutputStream outputStream) {
        final CompletableFuture<Long> answer = new CompletableFuture<>();
        final AtomicLong bytes = new AtomicLong(0);

        this.stub.snapshot(SnapshotRequest.getDefaultInstance())
            .handler(r -> {
                try {
                    r.getBlob().writeTo(outputStream);
                    bytes.addAndGet(r.getBlob().size());
                } catch (IOException e) {
                    answer.completeExceptionally(toEtcdException(e));
                }
            })
            .endHandler(event -> {
                answer.complete(bytes.get());
            })
            .exceptionHandler(e -> {
                answer.completeExceptionally(toEtcdException(e));
            });

        return answer;
    }

    @Override
    public void snapshot(StreamObserver<io.etcd.jetcd.maintenance.SnapshotResponse> observer) {

        this.stub.snapshot(SnapshotRequest.getDefaultInstance())
            .handler(r -> observer.onNext(new io.etcd.jetcd.maintenance.SnapshotResponse(r)))
            .endHandler(event -> observer.onCompleted())
            .exceptionHandler(e -> observer.onError(toEtcdException(e)));
    }
}
