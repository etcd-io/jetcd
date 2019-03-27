/*
 * Copyright 2016-2019 The jetcd authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import io.etcd.jetcd.api.lock.LockGrpc;
import io.etcd.jetcd.api.lock.LockRequest;
import io.etcd.jetcd.api.lock.UnlockRequest;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import java.util.concurrent.CompletableFuture;

final class LockImpl implements Lock {

  private final ClientConnectionManager connectionManager;

  private final LockGrpc.LockFutureStub stub;

  private final ByteSequence namespace;

  LockImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(LockGrpc::newFutureStub);
    this.namespace = connectionManager.getNamespace();
  }

  @Override
  public CompletableFuture<LockResponse> lock(ByteSequence name, long leaseId) {
    checkNotNull(name);
    LockRequest request = LockRequest.newBuilder()
        .setName(Util.prefixNamespace(name.getByteString(), namespace))
        .setLease(leaseId)
        .build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.lock(request),
        (response) -> new LockResponse(response, namespace),
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }

  @Override
  public CompletableFuture<UnlockResponse> unlock(ByteSequence lockKey) {
    checkNotNull(lockKey);
    UnlockRequest request = UnlockRequest.newBuilder()
        .setKey(Util.prefixNamespace(lockKey.getByteString(), namespace))
        .build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.unlock(request),
        UnlockResponse::new,
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }
}
