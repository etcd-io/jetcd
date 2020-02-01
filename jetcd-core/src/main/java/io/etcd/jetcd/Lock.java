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

import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
   * Acquire a lock with the given name.Waits if necessary for at most the given time
   * if etcd server is available.
   *
   * @param name
   *          the identifier for the distributed shared lock to be acquired.
   * @param leaseId
   *          the ID of the lease that will be attached to ownership of the
   *          lock. If the lease expires or is revoked and currently holds the
   *          lock, the lock is automatically released. Calls to Lock with the
   *          same lease will be treated as a single acquistion; locking twice
   *          with the same lease is a no-op.
   * @param timeout the maximum time to waits
   * @param unit the time unit of the timeout argument
   */
  CompletableFuture<LockResponse> lock(ByteSequence name, long leaseId, long timeout, TimeUnit unit);

  /**
   * Release the lock identified by the given key.
   *
   * @param lockKey
   *          key is the lock ownership key granted by Lock.
   */
  CompletableFuture<UnlockResponse> unlock(ByteSequence lockKey);

  /**
   * Release the lock identified by the given key.Waits if necessary for at most the given
   * time if etcd server is available.
   *
   * @param lockKey
   *          key is the lock ownership key granted by Lock.
   * @param timeout the maximum time to waits
   * @param unit the time unit of the timeout argument
   */
  CompletableFuture<UnlockResponse> unlock(ByteSequence lockKey, long timeout, TimeUnit unit);
}
