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

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedWatchClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedWatcherException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.api.WatchCancelRequest;
import io.etcd.jetcd.api.WatchCreateRequest;
import io.etcd.jetcd.api.WatchGrpc;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponseWithError;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * watch Implementation.
 */
class WatchImpl implements Watch {

  private static final Logger LOG = LoggerFactory.getLogger(WatchImpl.class);

  // watchers stores a mapping between leaseID -> WatchIml.
  private final ConcurrentHashMap<Long, WatcherImpl> watchers = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<WatcherImpl>
      pendingWatchers = new ConcurrentLinkedQueue<>();
  private final Set<Long> cancelSet = ConcurrentHashMap.newKeySet();
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final ScheduledExecutorService scheduledExecutorService = Executors
      .newScheduledThreadPool(1);
  private final ClientConnectionManager connectionManager;
  private final WatchGrpc.WatchStub stub;
  private volatile StreamObserver<WatchRequest> grpcWatchStreamObserver;
  private boolean closed = false;

  WatchImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(WatchGrpc::newStub);
  }

  private boolean isClosed() {
    return this.closed;
  }

  private void setClosed() {
    this.closed = true;
  }

  @Override
  public Watcher watch(ByteSequence key) {
    return this.watch(key, WatchOption.DEFAULT);
  }

  @Override
  public synchronized Watcher watch(ByteSequence key, WatchOption watchOption) {
    if (isClosed()) {
      throw newClosedWatchClientException();
    }

    WatcherImpl watcher = new WatcherImpl(key, watchOption, this);
    this.pendingWatchers.add(watcher);

    if (this.pendingWatchers.size() == 1) {
      // head of the queue send watchCreate request.
      WatchRequest request = this.toWatchCreateRequest(watcher);
      this.getGrpcWatchStreamObserver().onNext(request);
    }

    return watcher;
  }

  @Override
  public synchronized void close() {
    if (isClosed()) {
      return;
    }

    this.setClosed();
    this.notifyWatchers(newClosedWatchClientException());
    this.closeGrpcWatchStreamObserver();
    this.executor.shutdownNow();
    this.scheduledExecutorService.shutdownNow();
  }

  // notifies all watchers about a exception. it doesn't close watchers.
  // it is the responsibility of user to close watchers.
  private void notifyWatchers(EtcdException e) {
    WatchResponseWithError wre = new WatchResponseWithError(e);
    this.pendingWatchers.forEach(watcher -> {
      try {
        watcher.enqueue(wre);
      } catch (Exception we) {
        LOG.warn("failed to notify watcher", we);
      }
    });
    this.pendingWatchers.clear();
    this.watchers.values().forEach(watcher -> {
      try {
        watcher.enqueue(wre);
      } catch (Exception we) {
        LOG.warn("failed to notify watcher", we);
      }
    });
    this.watchers.clear();
  }

  private synchronized void cancelWatcher(long id) {
    if (this.isClosed()) {
      return;
    }

    if (this.cancelSet.contains(id)) {
      return;
    }

    this.watchers.remove(id);
    this.cancelSet.add(id);

    WatchCancelRequest watchCancelRequest = WatchCancelRequest.newBuilder().setWatchId(id).build();
    WatchRequest cancelRequest = WatchRequest.newBuilder().setCancelRequest(watchCancelRequest)
        .build();
    this.getGrpcWatchStreamObserver().onNext(cancelRequest);
  }

  private synchronized StreamObserver<WatchRequest> getGrpcWatchStreamObserver() {
    if (this.grpcWatchStreamObserver == null) {
      this.grpcWatchStreamObserver = this.stub.watch(this.createWatchStreamObserver());
    }
    return this.grpcWatchStreamObserver;
  }

  private StreamObserver<WatchResponse> createWatchStreamObserver() {
    return new StreamObserver<WatchResponse>() {
      @Override
      public void onNext(WatchResponse watchResponse) {
        processWatchResponse(watchResponse);
      }

      @Override
      public void onError(Throwable t) {
        processError(t);
      }

      @Override
      public void onCompleted() {
      }
    };
  }

  private synchronized void processWatchResponse(WatchResponse watchResponse) {
    // prevents grpc on sending watchResponse to a closed watch client.
    if (this.isClosed()) {
      return;
    }

    if (watchResponse.getCreated()) {
      processCreate(watchResponse);
    } else if (watchResponse.getCanceled()) {
      processCanceled(watchResponse);
    } else {
      processEvents(watchResponse);
    }
  }

  private synchronized void processError(Throwable t) {
    // prevents grpc on sending error to a closed watch client.
    if (this.isClosed()) {
      return;
    }

    Status status = Status.fromThrowable(t);
    if (this.isHaltError(status) || this.isNoLeaderError(status)) {
      this.notifyWatchers(toEtcdException(status));
      this.closeGrpcWatchStreamObserver();
      this.cancelSet.clear();
      return;
    }
    // resume with a delay; avoiding immediate retry on a long connection downtime.
    scheduledExecutorService.schedule(this::resume, 500, TimeUnit.MILLISECONDS);
  }

  private synchronized void resume() {
    this.closeGrpcWatchStreamObserver();
    this.cancelSet.clear();
    this.resumeWatchers();
  }

  // closeGrpcWatchStreamObserver closes the underlying grpc watch stream.
  private void closeGrpcWatchStreamObserver() {
    if (this.grpcWatchStreamObserver == null) {
      return;
    }
    this.grpcWatchStreamObserver.onCompleted();
    this.grpcWatchStreamObserver = null;
  }

  private boolean isNoLeaderError(Status status) {
    return status.getCode() == Code.UNAVAILABLE
        && "etcdserver: no leader".equals(status.getDescription());
  }

  private boolean isHaltError(Status status) {
    // Unavailable codes mean the system will be right back.
    // (e.g., can't connect, lost leader)
    // Treat Internal codes as if something failed, leaving the
    // system in an inconsistent state, but retrying could make progress.
    // (e.g., failed in middle of send, corrupted frame)
    return status.getCode() != Code.UNAVAILABLE && status.getCode() != Code.INTERNAL;
  }

  private void processCreate(WatchResponse response) {
    WatcherImpl watcher = this.pendingWatchers.poll();

    this.sendNextWatchCreateRequest();

    if (watcher == null) {
      // shouldn't happen
      // may happen due to duplicate watch create responses.
      LOG.warn("Watch client receives watch create response but find no corresponding watcher");
      return;
    }

    if (watcher.isClosed()) {
      return;
    }

    if (response.getWatchId() == -1) {
      watcher.enqueue(new WatchResponseWithError(
          newEtcdException(ErrorCode.INTERNAL, "etcd server failed to create watch id")));
      return;
    }

    if (watcher.getRevision() == 0) {
      watcher.setRevision(response.getHeader().getRevision());
    }

    watcher.setWatchID(response.getWatchId());
    this.watchers.put(watcher.getWatchID(), watcher);
  }

  /**
   * chooses the next resuming watcher to register with the grpc stream.
   */
  private Optional<WatchRequest> nextResume() {
    WatcherImpl pendingWatcher = this.pendingWatchers.peek();
    if (pendingWatcher != null) {
      return Optional.of(this.toWatchCreateRequest(pendingWatcher));
    }
    return Optional.empty();
  }

  private void sendNextWatchCreateRequest() {
    this.nextResume().ifPresent(
        nextWatchRequest -> this.getGrpcWatchStreamObserver().onNext(nextWatchRequest));
  }

  private void processEvents(WatchResponse response) {
    WatcherImpl watcher = this.watchers.get(response.getWatchId());
    if (watcher == null) {
      // cancel server side watcher.
      this.cancelWatcher(response.getWatchId());
      return;
    }

    if (response.getCompactRevision() != 0) {
      watcher.enqueue(new WatchResponseWithError(
          EtcdExceptionFactory
              .newCompactedException(response.getCompactRevision())));
      return;
    }

    if (response.getEventsCount() == 0) {
      watcher.setRevision(response.getHeader().getRevision());
      return;
    }

    watcher.enqueue(new WatchResponseWithError(response));
    watcher.setRevision(
        response
            .getEvents(response.getEventsCount() - 1)
            .getKv().getModRevision() + 1);
  }

  private void resumeWatchers() {
    this.watchers.values().forEach(watcher -> {
      if (watcher.isClosed()) {
        return;
      }
      watcher.setWatchID(-1);
      this.pendingWatchers.add(watcher);
    });

    this.watchers.clear();

    this.sendNextWatchCreateRequest();
  }

  private void processCanceled(WatchResponse response) {
    WatcherImpl watcher = this.watchers.get(response.getWatchId());
    this.cancelSet.remove(response.getWatchId());
    if (watcher == null) {
      return;
    }
    String reason = response.getCancelReason();
    if (Strings.isNullOrEmpty(reason)) {
      watcher.enqueue(new WatchResponseWithError(newEtcdException(
          ErrorCode.OUT_OF_RANGE,
          "etcdserver: mvcc: required revision is a future revision"))
      );

    } else {
      watcher.enqueue(
          new WatchResponseWithError(newEtcdException(ErrorCode.FAILED_PRECONDITION, reason)));
    }
  }

  private WatchRequest toWatchCreateRequest(WatcherImpl watcher) {
    ByteString key = watcher.getKey().getByteString();
    WatchOption option = watcher.getWatchOption();
    WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
        .setKey(key)
        .setPrevKv(option.isPrevKV())
        .setProgressNotify(option.isProgressNotify())
        .setStartRevision(watcher.getRevision());

    option.getEndKey()
      .map(ByteSequence::getByteString)
      .ifPresent(builder::setRangeEnd);

    if (option.isNoDelete()) {
      builder.addFilters(WatchCreateRequest.FilterType.NODELETE);
    }

    if (option.isNoPut()) {
      builder.addFilters(WatchCreateRequest.FilterType.NOPUT);
    }

    return WatchRequest.newBuilder().setCreateRequest(builder).build();
  }

  /**
   * Watcher class holds watcher information.
   */
  public static class WatcherImpl implements Watcher {

    final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final WatchOption watchOption;
    private final ByteSequence key;
    private final Object closedLock = new Object();
    // watch events buffer.
    private final BlockingQueue<WatchResponseWithError> eventsQueue = new LinkedBlockingQueue<>();
    private long watchID;
    // the revision to watch on.
    private long revision;
    private boolean closed = false;
    private final WatchImpl owner;

    private WatcherImpl(ByteSequence key, WatchOption watchOption, WatchImpl owner) {
      this.key = key;
      this.watchOption = watchOption;
      this.revision = watchOption.getRevision();
      this.owner = owner;
    }

    private long getRevision() {
      return this.revision;
    }

    private void setRevision(long revision) {
      this.revision = revision;
    }

    public boolean isClosed() {
      synchronized (this.closedLock) {
        return closed;
      }
    }

    private void setClosed() {
      synchronized (this.closedLock) {
        this.closed = true;
      }
    }

    private long getWatchID() {
      return watchID;
    }

    private void setWatchID(long watchID) {
      this.watchID = watchID;
    }

    private WatchOption getWatchOption() {
      return watchOption;
    }

    private ByteSequence getKey() {
      return key;
    }

    private void enqueue(WatchResponseWithError watchResponse) {
      try {
        this.eventsQueue.put(watchResponse);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.warn("Interrupted", e);
      }
    }

    @Override
    public void close() {
      synchronized (this.closedLock) {
        if (isClosed()) {
          return;
        }
        this.setClosed();
      }

      this.owner.cancelWatcher(this.watchID);
      this.executor.shutdownNow();
    }

    @Override
    public synchronized io.etcd.jetcd.watch.WatchResponse listen() throws InterruptedException {
      if (isClosed()) {
        throw newClosedWatcherException();
      }

      try {
        return this.createWatchResponseFuture().get();
      } catch (ExecutionException e) {
        synchronized (this.closedLock) {
          if (isClosed()) {
            throw newClosedWatcherException();
          }
        }
        Throwable t = e.getCause();
        if (t instanceof EtcdException) {
          throw (EtcdException) t;
        }
        throw toEtcdException(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw e;
      } catch (RejectedExecutionException e) {
        throw newClosedWatcherException();
      }
    }

    private Future<io.etcd.jetcd.watch.WatchResponse> createWatchResponseFuture() {
      return this.executor.submit(() -> {
        WatchResponseWithError watchResponse = this.eventsQueue.take();
        if (watchResponse.getException() != null) {
          throw watchResponse.getException();
        }
        return new io.etcd.jetcd.watch.WatchResponse(watchResponse.getWatchResponse());
      });
    }
  }
}

