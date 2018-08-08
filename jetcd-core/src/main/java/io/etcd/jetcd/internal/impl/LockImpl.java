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

import static com.google.common.base.Preconditions.checkNotNull;

import io.etcd.jetcd.Lock;
import io.etcd.jetcd.api.lock.LockGrpc;
import io.etcd.jetcd.api.lock.LockRequest;
import io.etcd.jetcd.api.lock.UnlockRequest;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import java.util.concurrent.CompletableFuture;

class LockImpl implements Lock {

  private final ClientConnectionManager connectionManager;

  private final LockGrpc.LockFutureStub stub;

  LockImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(LockGrpc::newFutureStub);
  }

  @Override
  public CompletableFuture<LockResponse> lock(ByteSequence name, long leaseId) {
    checkNotNull(name);
    LockRequest request = LockRequest.newBuilder()
        .setName(name.getByteString())
        .setLease(leaseId)
        .build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.lock(request),
        LockResponse::new,
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }

  @Override
  public CompletableFuture<UnlockResponse> unlock(ByteSequence lockKey) {
    checkNotNull(lockKey);
    UnlockRequest request = UnlockRequest.newBuilder()
        .setKey(lockKey.getByteString())
        .build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.unlock(request),
        UnlockResponse::new,
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }
}
