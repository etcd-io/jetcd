package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Watch;
import com.coreos.jetcd.api.WatchCancelRequest;
import com.coreos.jetcd.api.WatchCreateRequest;
import com.coreos.jetcd.api.WatchGrpc;
import com.coreos.jetcd.api.WatchRequest;
import com.coreos.jetcd.api.WatchResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.CompactedException;
import com.coreos.jetcd.exception.EtcdException;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchResponseWithError;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import io.grpc.Status;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * watch Implementation.
 */
class WatchImpl implements Watch {

  private static final Logger logger = Logger.getLogger(WatchImpl.class.getName());

  private volatile StreamObserver<WatchRequest> grpcWatchStreamObserver;

  // watchers stores a mapping between leaseID -> WatchIml.
  private final ConcurrentHashMap<Long, WatcherImpl> watchers = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<WatcherImpl>
      pendingWatchers = new ConcurrentLinkedQueue<>();

  private final Set<Long> cancelSet = ConcurrentHashMap.newKeySet();

  private boolean closed = false;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  private final ScheduledExecutorService scheduledExecutorService = Executors
      .newScheduledThreadPool(1);

  private final ClientConnectionManager connectionManager;
  private final WatchGrpc.WatchStub stub;

  private boolean isClosed() {
    return this.closed;
  }

  private void setClosed() {
    this.closed = true;
  }

  WatchImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(WatchGrpc::newStub);
  }

  @Override
  public Watcher watch(ByteSequence key) {
    return this.watch(key, WatchOption.DEFAULT);
  }

  @Override
  public synchronized Watcher watch(ByteSequence key, WatchOption watchOption) {
    Preconditions.checkState(!isClosed(), "Watch client has been closed");

    WatcherImpl watcher = new WatcherImpl(key, watchOption);
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
    Preconditions.checkState(!isClosed(), "Watch client has been closed");

    this.setClosed();
    this.notifyWatchers(EtcdExceptionFactory.newEtcdException("Watch client has been closed"));
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
        logger.log(Level.WARNING, "failed to notify watcher", we);
      }
    });
    this.pendingWatchers.clear();
    this.watchers.values().forEach(watcher -> {
      try {
        watcher.enqueue(wre);
      } catch (Exception we) {
        logger.log(Level.WARNING, "failed to notify watcher", we);
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
      this.notifyWatchers(EtcdExceptionFactory.newEtcdException(status.getDescription()));
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
    if (status.getDescription() == null) {
      return false;
    }
    return status.getDescription().contains("no leader");
  }

  private boolean isHaltError(Status status) {
    // Unavailable codes mean the system will be right back.
    // (e.g., can't connect, lost leader)
    // Treat Internal codes as if something failed, leaving the
    // system in an inconsistent state, but retrying could make progress.
    // (e.g., failed in middle of send, corrupted frame)
    return status.getCode() != Status.Code.UNAVAILABLE && status.getCode() != Status.Code.INTERNAL;
  }

  private void processCreate(WatchResponse response) {
    WatcherImpl watcher = this.pendingWatchers.poll();

    this.sendNextWatchCreateRequest();

    if (watcher == null) {
      // shouldn't happen
      // may happen due to duplicate watch create responses.
      logger.log(Level.WARNING,
          "Watch client receives watch create response but find no corresponding watcher");
      return;
    }

    if (watcher.isClosed()) {
      return;
    }

    if (response.getWatchId() == -1) {
      watcher.enqueue(new WatchResponseWithError(
          EtcdExceptionFactory.newEtcdException(("etcd server failed to create watch id"))));
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
        (nextWatchRequest -> this.getGrpcWatchStreamObserver().onNext(nextWatchRequest)));
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
          EtcdExceptionFactory.newCompactedException(response.getCompactRevision())));
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
    if (reason != null && reason.isEmpty() || reason == null) {
      reason = "required revision is a future revision";
    }
    watcher.enqueue(new WatchResponseWithError(
        EtcdExceptionFactory.newEtcdException(reason)));
  }

  private WatchRequest toWatchCreateRequest(WatcherImpl watcher) {
    ByteString key = Util.byteStringFromByteSequence(watcher.getKey());
    WatchOption option = watcher.getWatchOption();
    WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
        .setKey(key)
        .setPrevKv(option.isPrevKV())
        .setProgressNotify(option.isProgressNotify())
        .setStartRevision(watcher.getRevision());

    option.getEndKey()
        .ifPresent(endKey -> builder.setRangeEnd(Util.byteStringFromByteSequence(endKey)));

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
  public class WatcherImpl implements Watcher {

    private final WatchOption watchOption;
    private final ByteSequence key;
    private long watchID;
    // the revision to watch on.
    private long revision;
    private final Object closedLock = new Object();
    private boolean closed = false;

    // watch events buffer.
    private final BlockingQueue<WatchResponseWithError> eventsQueue = new LinkedBlockingQueue<>();

    final ExecutorService executor = Executors.newSingleThreadExecutor();

    private WatcherImpl(ByteSequence key, WatchOption watchOption) {
      this.key = key;
      this.watchOption = watchOption;
      this.revision = watchOption.getRevision();
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
        logger.log(Level.WARNING, "Interrupted", e);
      }
    }

    @Override
    public void close() {
      synchronized (this.closedLock) {
        Preconditions.checkState(!isClosed(), "Watcher has been closed");

        this.setClosed();
      }

      cancelWatcher(this.watchID);
      this.executor.shutdownNow();
    }

    @Override
    public synchronized com.coreos.jetcd.watch.WatchResponse listen() {
      Preconditions.checkState(!isClosed(), "Watcher has been closed");

      try {
        return this.createWatchResponseFuture().get();
      } catch (ExecutionException e) {
        Throwable t = e.getCause();
        if (t instanceof CompactedException) {
          throw (CompactedException) t;
        }
        if (t instanceof EtcdException) {
          throw (EtcdException) t;
        }
        throw EtcdExceptionFactory.newEtcdException(t);
      } catch (InterruptedException e) {
        throw EtcdExceptionFactory.handleInterrupt(e);
      } catch (Exception e) {
        throw EtcdExceptionFactory.newEtcdException(e);
      }
    }

    private Future<com.coreos.jetcd.watch.WatchResponse> createWatchResponseFuture() {
      return this.executor.submit(() -> {
        WatchResponseWithError watchResponse = this.eventsQueue.take();
        if (watchResponse.getException() != null) {
          throw watchResponse.getException();
        }
        return Util.toWatchResponse(watchResponse.getWatchResponse());
      });
    }
  }
}

