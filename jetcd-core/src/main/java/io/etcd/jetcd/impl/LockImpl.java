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

import java.util.concurrent.CompletableFuture;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.api.lock.LockRequest;
import io.etcd.jetcd.api.lock.UnlockRequest;
import io.etcd.jetcd.api.lock.VertxLockGrpc;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.support.Util;

import static java.util.Objects.requireNonNull;

final class LockImpl extends Impl implements Lock {
    private final VertxLockGrpc.LockVertxStub stub;
    private final ByteSequence namespace;

    // Lock operations are done in a context where a client is trying to implement
    // some strict mutual exclusion use case; in that type of context, it makes sense to always
    // apply require leader, since we don't want a client connected to a non-raft-leader server
    // to have a lock method just go silent if the server the client happens to be connected to
    // becomes partitioned from the actual raft-leader server in the etcd servers cluster:
    // in that scenario and without required leader, an attempt to lock could block forever
    // not because some other client is already holding a lock, but because the server the client
    // is connected to is partitioned and can't tell.
    // With require leader, in that case the call will fail and the client has the ability to
    // (a) know (b) retry on a different server.
    // The retry on a different server should happen automatically if the connection manager is using
    // a round robin strategy.
    private VertxLockGrpc.LockVertxStub stubWithLeader() {
        return Util.applyRequireLeader(true, stub);
    }

    LockImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager.newStub(VertxLockGrpc::newVertxStub);
        this.namespace = connectionManager.getNamespace();
    }

    @Override
    public CompletableFuture<LockResponse> lock(ByteSequence name, long leaseId) {
        requireNonNull(name);

        LockRequest request = LockRequest.newBuilder()
            .setName(Util.prefixNamespace(name, namespace))
            .setLease(leaseId)
            .build();

        return execute(
            () -> stubWithLeader().lock(request),
            response -> new LockResponse(response, namespace),
            Errors::isRetryable);
    }

    @Override
    public CompletableFuture<UnlockResponse> unlock(ByteSequence lockKey) {
        requireNonNull(lockKey);

        UnlockRequest request = UnlockRequest.newBuilder()
            .setKey(Util.prefixNamespace(lockKey, namespace))
            .build();

        return execute(
            () -> stubWithLeader().unlock(request),
            UnlockResponse::new,
            Errors::isRetryable);
    }
}
