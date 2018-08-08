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

package io.etcd.jetcd;

import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.internal.impl.CloseableClient;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of Lock talking to etcd.
 */
public interface Lock extends CloseableClient {

  /**
   * Acquire a lock with the given name.
   *
   * @param name
   *          the identifier for the distributed shared lock to be acquired.
   * @param leaseId
   *          the ID of the lease that will be attached to ownership of the
   *          lock. If the lease expires or is revoked and currently holds the
   *          lock, the lock is automatically released. Calls to Lock with the
   *          same lease will be treated as a single acquistion; locking twice
   *          with the same lease is a no-op.
   */
  CompletableFuture<LockResponse> lock(ByteSequence name, long leaseId);

  /**
   * Release the lock identified by the given key.
   *
   * @param lockKey
   *          key is the lock ownership key granted by Lock.
   */
  CompletableFuture<UnlockResponse> unlock(ByteSequence lockKey);

}
