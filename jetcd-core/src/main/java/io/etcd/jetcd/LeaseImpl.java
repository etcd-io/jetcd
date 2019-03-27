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
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedLeaseClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.etcd.jetcd.api.LeaseGrantRequest;
import io.etcd.jetcd.api.LeaseGrpc;
import io.etcd.jetcd.api.LeaseKeepAliveRequest;
import io.etcd.jetcd.api.LeaseRevokeRequest;
import io.etcd.jetcd.api.LeaseTimeToLiveRequest;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.LeaseOption;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of lease client.
 */
final class LeaseImpl implements Lease {

  private static final Logger LOG = LoggerFactory.getLogger(LeaseImpl.class);

  /**
   * FIRST_KEEPALIVE_TIMEOUT_MS is the timeout for the first keepalive request
   * before the actual TTL is known to the lease client.
   */
  private static final int FIRST_KEEPALIVE_TIMEOUT_MS = 5000;

  private final ClientConnectionManager connectionManager;
  private final LeaseGrpc.LeaseFutureStub stub;
  private final LeaseGrpc.LeaseStub leaseStub;
  private final Map<Long, KeepAlive> keepAlives;
  /**
   * Timer schedule to send keep alive request.
   */
  private final ListeningScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture<?> keepAliveFuture;
  private ScheduledFuture<?> deadlineFuture;
  /**
   * KeepAlive Request Stream, put request into this stream to keep the lease alive.
   */
  private StreamObserver<LeaseKeepAliveRequest> keepAliveRequestObserver;

  /**
   * KeepAlive Response Streamer, receive keep alive response from this stream and update the
   * nextKeepAliveTime and deadline of the leases.
   */
  private StreamObserver<io.etcd.jetcd.api.LeaseKeepAliveResponse> keepAliveResponseObserver;

  /**
   * hasKeepAliveServiceStarted indicates whether the background keep alive service has started.
   */
  private volatile boolean hasKeepAliveServiceStarted;
  private volatile boolean closed;

  LeaseImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(LeaseGrpc::newFutureStub);
    this.leaseStub = connectionManager.newStub(LeaseGrpc::newStub);
    this.keepAlives = new ConcurrentHashMap<>();
    this.scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(2));
  }

  @Override
  public CompletableFuture<LeaseGrantResponse> grant(long ttl) {
    LeaseGrantRequest leaseGrantRequest = LeaseGrantRequest.newBuilder().setTTL(ttl).build();
    return Util.toCompletableFutureWithRetry(
        () -> this.stub.leaseGrant(leaseGrantRequest),
        LeaseGrantResponse::new,
        connectionManager.getExecutorService()
    );
  }

  @Override
  public CompletableFuture<LeaseRevokeResponse> revoke(long leaseId) {
    LeaseRevokeRequest leaseRevokeRequest = LeaseRevokeRequest.newBuilder().setID(leaseId).build();
    return Util.toCompletableFutureWithRetry(
        () -> this.stub.leaseRevoke(leaseRevokeRequest),
        LeaseRevokeResponse::new,
        connectionManager.getExecutorService()
    );
  }

  @Override
  public synchronized CloseableClient keepAlive(long leaseId, StreamObserver<LeaseKeepAliveResponse> observer) {
    if (this.closed) {
      throw newClosedLeaseClientException();
    }

    KeepAlive keepAlive = this.keepAlives.computeIfAbsent(leaseId, (key) -> new KeepAlive(leaseId));
    keepAlive.addObserver(observer);

    if (!this.hasKeepAliveServiceStarted) {
      this.hasKeepAliveServiceStarted = true;
      this.start();
    }

    return new CloseableClient() {
      @Override
      public void close() {
        keepAlive.removeObserver(observer);
      }
    };
  }

  @Override
  public synchronized void close() {
    if (this.closed) {
      return;
    }
    this.closed = true;

    if (!this.hasKeepAliveServiceStarted) { // hasKeepAliveServiceStarted hasn't started.
      return;
    }

    this.keepAliveFuture.cancel(true);
    this.deadlineFuture.cancel(true);
    this.keepAliveRequestObserver.onCompleted();
    this.keepAliveResponseObserver.onCompleted();
    this.scheduledExecutorService.shutdownNow();

    final Throwable errResp = newClosedLeaseClientException();

    this.keepAlives.forEach((k, v) -> v.onError(errResp));
    this.keepAlives.clear();
  }

  private synchronized void removeKeepAlive(long leaseId) {
    this.keepAlives.remove(leaseId);
  }

  private void start() {
    this.sendKeepAliveExecutor();
    this.deadLineExecutor();
  }

  private void reset() {
    this.keepAliveFuture.cancel(true);
    this.keepAliveRequestObserver.onCompleted();
    this.keepAliveResponseObserver.onCompleted();
    this.sendKeepAliveExecutor();
  }

  private void sendKeepAliveExecutor() {
    this.keepAliveResponseObserver = Observers.observer(
      response -> processKeepAliveResponse(response),
      error -> processOnError()
    );

    this.keepAliveRequestObserver = this.leaseStub.leaseKeepAlive(this.keepAliveResponseObserver);
    this.keepAliveFuture = scheduledExecutorService.scheduleAtFixedRate(
        () -> {
            // send keep alive req to the leases whose next keep alive is before now.
            this.keepAlives.entrySet().stream()
                .filter(entry -> entry.getValue().getNextKeepAlive() < System.currentTimeMillis())
                .map(Entry::getKey)
                .map(leaseId -> LeaseKeepAliveRequest.newBuilder().setID(leaseId).build())
                .forEach(keepAliveRequestObserver::onNext);

        },
        0,
        500,
        TimeUnit.MILLISECONDS
    );
  }

  private synchronized void processOnError() {
    if (this.closed) {
      return;
    }

    Util.addOnFailureLoggingCallback(
        this.connectionManager.getExecutorService(),
        this.scheduledExecutorService.schedule(() -> reset(), 500, TimeUnit.MILLISECONDS),
        LOG,
        "scheduled reset failed"
    );
  }

  private synchronized void processKeepAliveResponse(
      io.etcd.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse) {
    if (this.closed) {
      return;
    }

    final long leaseID = leaseKeepAliveResponse.getID();
    final long ttl = leaseKeepAliveResponse.getTTL();
    final KeepAlive ka = this.keepAlives.get(leaseID);

    if (ka == null) {
      // return if the corresponding keep alive has closed.
      return;
    }

    if (ttl > 0) {
      long nextKeepAlive = System.currentTimeMillis() + ttl * 1000 / 3;
      ka.setNextKeepAlive(nextKeepAlive);
      ka.setDeadLine(System.currentTimeMillis() + ttl * 1000);
      ka.onNext(leaseKeepAliveResponse);
    } else {
      // lease expired; close all keep alive
      this.removeKeepAlive(leaseID);
      ka.onError(
          newEtcdException(
            ErrorCode.NOT_FOUND,
            "etcdserver: requested lease not found"
          )
      );
    }
  }

  private void deadLineExecutor() {
    this.deadlineFuture = scheduledExecutorService.scheduleAtFixedRate(
      () -> {
          long now = System.currentTimeMillis();

          this.keepAlives.values().removeIf(ka -> {
            if (ka.getDeadLine() < now) {
              ka.onCompleted();
              return true;
            }
            return false;
          });
        },
        0,
        1000,
        TimeUnit.MILLISECONDS
    );
  }

  @Override
  public CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId) {
    CompletableFuture<LeaseKeepAliveResponse> future = new CompletableFuture<>();

    StreamObserver<LeaseKeepAliveRequest> requestObserver = Observers.observe(
        this.leaseStub::leaseKeepAlive,
        response  -> future.complete(new LeaseKeepAliveResponse(response)),
        throwable -> future.completeExceptionally(toEtcdException(throwable))
    );

    // cancel grpc stream when leaseKeepAliveResponseCompletableFuture completes.
    CompletableFuture<LeaseKeepAliveResponse> answer = future.whenCompleteAsync(
        (val, throwable) -> requestObserver.onCompleted(),
        connectionManager.getExecutorService()
    );

    requestObserver.onNext(LeaseKeepAliveRequest.newBuilder().setID(leaseId).build());

    return answer;
  }

  @Override
  public CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId, LeaseOption option) {
    checkNotNull(option, "LeaseOption should not be null");

    LeaseTimeToLiveRequest leaseTimeToLiveRequest = LeaseTimeToLiveRequest.newBuilder()
        .setID(leaseId)
        .setKeys(option.isAttachedKeys())
        .build();

    return Util.toCompletableFutureWithRetry(
        () -> this.stub.leaseTimeToLive(leaseTimeToLiveRequest),
        LeaseTimeToLiveResponse::new,
        connectionManager.getExecutorService());
  }

  /**
   * The KeepAlive hold the keepAlive information for lease.
   */
  private final class KeepAlive implements StreamObserver<io.etcd.jetcd.api.LeaseKeepAliveResponse> {
    private final List<StreamObserver<LeaseKeepAliveResponse>> observers;
    private final long leaseId;

    private long deadLine;
    private long nextKeepAlive;

    public KeepAlive(long leaseId) {
      this.nextKeepAlive = System.currentTimeMillis();
      this.deadLine = nextKeepAlive + FIRST_KEEPALIVE_TIMEOUT_MS;

      this.observers = new CopyOnWriteArrayList<>();
      this.leaseId = leaseId;
    }

    public long getDeadLine() {
      return deadLine;
    }

    public void setDeadLine(long deadLine) {
      this.deadLine = deadLine;
    }

    public void addObserver(StreamObserver<LeaseKeepAliveResponse> observer) {
      this.observers.add(observer);
    }

    //removeObserver only would be called synchronously by close in KeepAliveListener, no need to get lock here
    public void removeObserver(StreamObserver<LeaseKeepAliveResponse> listener) {
      this.observers.remove(listener);
      if (this.observers.isEmpty()) {
        removeKeepAlive(leaseId);
      }
    }

    public long getNextKeepAlive() {
      return nextKeepAlive;
    }

    public void setNextKeepAlive(long nextKeepAlive) {
      this.nextKeepAlive = nextKeepAlive;
    }

    @Override
    public void onNext(io.etcd.jetcd.api.LeaseKeepAliveResponse response) {
      for (StreamObserver<LeaseKeepAliveResponse> observer : observers) {
        observer.onNext(new LeaseKeepAliveResponse(response));
      }
    }

    @Override
    public void onError(Throwable throwable) {
      for (StreamObserver<LeaseKeepAliveResponse> observer : observers) {
        observer.onError(toEtcdException(throwable));
      }
    }

    @Override
    public void onCompleted() {
      this.observers.forEach(StreamObserver::onCompleted);
      this.observers.clear();
    }
  }
}
