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

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.maintenance.AlarmResponse;
import io.etcd.jetcd.maintenance.DefragmentResponse;
import io.etcd.jetcd.maintenance.HashKVResponse;
import io.etcd.jetcd.maintenance.HashResponse;
import io.etcd.jetcd.maintenance.MoveLeaderResponse;
import io.etcd.jetcd.maintenance.SnapshotResponse;
import io.etcd.jetcd.maintenance.StatusResponse;
import io.vertx.core.Future;
import io.vertx.grpc.client.GrpcClientRequestProvider;

import static io.etcd.jetcd.common.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of maintenance client.
 */
final class MaintenanceClient extends AbstractClient implements Maintenance {
    private final GrpcClientRequestProvider requests;
    private final io.etcd.jetcd.api.MaintenanceGrpcClient client;

    MaintenanceClient(GrpcService grpcService) {
        super(grpcService);

        requests = grpcService.getAuthenticatedGrpcClient().bind(grpcService.getServiceResolver().getTarget());
        client = io.etcd.jetcd.api.MaintenanceGrpcClient.create(requests);
    }

    @Override
    public CompletableFuture<AlarmResponse> listAlarms() {
        io.etcd.jetcd.api.AlarmRequest alarmRequest = io.etcd.jetcd.api.AlarmRequest.newBuilder()
            .setAlarm(io.etcd.jetcd.api.AlarmType.NONE)
            .setAction(io.etcd.jetcd.api.AlarmRequest.AlarmAction.GET)
            .setMemberID(0)
            .build();

        return completable(client.alarm(alarmRequest), AlarmResponse::new);
    }

    @Override
    public CompletableFuture<AlarmResponse> alarmDisarm(io.etcd.jetcd.maintenance.AlarmMember member) {
        checkArgument(member.memberId() != 0, "the member id can not be 0");
        checkArgument(member.alarmType() != io.etcd.jetcd.maintenance.AlarmType.NONE, "alarm type can not be NONE");

        io.etcd.jetcd.api.AlarmRequest alarmRequest = io.etcd.jetcd.api.AlarmRequest.newBuilder()
            .setAlarm(io.etcd.jetcd.api.AlarmType.NOSPACE)
            .setAction(io.etcd.jetcd.api.AlarmRequest.AlarmAction.DEACTIVATE)
            .setMemberID(member.memberId())
            .build();

        return completable(client.alarm(alarmRequest), AlarmResponse::new);
    }

    @Override
    public CompletableFuture<DefragmentResponse> defragmentMember(String target) {
        return executeForMember(target,
            member -> member.defragment(io.etcd.jetcd.api.DefragmentRequest.getDefaultInstance()),
            DefragmentResponse::new);
    }

    @Override
    public CompletableFuture<StatusResponse> statusMember(String target) {
        return executeForMember(target,
            member -> member.status(io.etcd.jetcd.api.StatusRequest.getDefaultInstance()),
            StatusResponse::new);
    }

    @Override
    public CompletableFuture<MoveLeaderResponse> moveLeader(long transfereeID) {
        return completable(
            client.moveLeader(io.etcd.jetcd.api.MoveLeaderRequest.newBuilder().setTargetID(transfereeID).build()),
            MoveLeaderResponse::new);
    }

    @Override
    public CompletableFuture<HashResponse> hash(String target) {
        return executeForMember(target,
            member -> member.hash(io.etcd.jetcd.api.HashRequest.getDefaultInstance()),
            HashResponse::new);
    }

    @Override
    public CompletableFuture<HashKVResponse> hashKV(String target, long rev) {
        return executeForMember(target,
            member -> member.hashKV(io.etcd.jetcd.api.HashKVRequest.newBuilder().setRevision(rev).build()),
            HashKVResponse::new);
    }

    private <S, T> CompletableFuture<T> executeForMember(String target,
        Function<io.etcd.jetcd.api.MaintenanceGrpcClient, Future<S>> operation, Function<S, T> resultConvert) {
        try {
            var member = io.etcd.jetcd.api.MaintenanceGrpcClient.create(grpc().bindDirect(target));
            return completable(operation.apply(member), resultConvert);
        } catch (IllegalArgumentException | ClosedClientException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Long> snapshot(OutputStream outputStream) {
        var bytes = new AtomicLong();
        return completable(transferSnapshot(chunk -> {
            var blob = chunk.getBlob();
            blob.writeTo(outputStream);
            bytes.addAndGet(blob.size());
        }), ignored -> bytes.get());
    }

    @Override
    public void snapshot(Maintenance.Listener listener) {
        requireNonNull(listener, "listener should not be null");
        ServerStreamingCall.notifyListener(grpc().vertx(),
            transferSnapshot(chunk -> listener.onNext(new SnapshotResponse(chunk))),
            listener::onError, listener::onCompleted);
    }

    private Future<Void> transferSnapshot(ServerStreamingCall.MessageHandler<io.etcd.jetcd.api.SnapshotResponse> handler) {
        return ServerStreamingCall.consume(requests, io.etcd.jetcd.api.MaintenanceGrpcClient.Snapshot,
            io.etcd.jetcd.api.SnapshotRequest.getDefaultInstance(), handler);
    }
}
