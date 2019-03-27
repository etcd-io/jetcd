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

import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.LeaseOption;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of KeepAlive talking to etcd.
 */
public interface Lease extends CloseableClient {

  /**
   * New a lease with ttl value.
   *
   * @param ttl ttl value, unit seconds
   */
  CompletableFuture<LeaseGrantResponse> grant(long ttl);

  /**
   * revoke one lease and the key bind to this lease will be removed.
   *
   * @param leaseId id of the lease to revoke
   */
  CompletableFuture<LeaseRevokeResponse> revoke(long leaseId);

  /**
   * keep alive one lease only once.
   *
   * @param leaseId id of lease to keep alive once
   * @return The keep alive response
   */
  CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId);

  /**
   * retrieves the lease information of the given lease ID.
   *
   * @param leaseId id of lease
   * @param leaseOption LeaseOption
   * @return LeaseTimeToLiveResponse wrapped in CompletableFuture
   */
  CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId,
      LeaseOption leaseOption);

  /**
   * keep the given lease alive forever.
   *
   * @param leaseId lease to be keep alive forever.
   * @param observer the observer
   * @return a KeepAliveListener that listens for KeepAlive responses.
   */
  CloseableClient keepAlive(long leaseId, StreamObserver<LeaseKeepAliveResponse> observer);
}
