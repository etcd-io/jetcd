package com.coreos.jetcd;

import static com.coreos.jetcd.Util.listenableToCompletableFuture;
import static com.coreos.jetcd.Util.toLeaseKeepAliveResponse;
import static com.google.common.base.Preconditions.checkNotNull;

import com.coreos.jetcd.api.LeaseGrantRequest;
import com.coreos.jetcd.api.LeaseGrpc;
import com.coreos.jetcd.api.LeaseKeepAliveRequest;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.api.LeaseRevokeRequest;
import com.coreos.jetcd.api.LeaseTimeToLiveRequest;
import com.coreos.jetcd.lease.KeepAlive;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseRevokeResponse;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.lease.NoSuchLeaseException;
import com.coreos.jetcd.options.LeaseOption;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of lease client.
 */
public class LeaseImpl implements Lease {

  private static final int DEFAULT_TTL = 5000;
  private static final int DEFAULT_SCAN_PERIOD = 500;
  private final LeaseGrpc.LeaseFutureStub leaseFutureStub;
  private final LeaseGrpc.LeaseStub leaseStub;
  /**
   * gRPC channel.
   */
  private ManagedChannel channel;
  /**
   * Timer schedule to send keep alive request.
   */
  private ScheduledExecutorService keepAliveSchedule;
  private ScheduledFuture<?> scheduledFuture;
  private long scanPeriod;

  private final Map<Long, KeepAlive> keepAlives = new ConcurrentHashMap<>();

  /**
   * The first time interval.
   */
  private long firstKeepAliveTimeOut = DEFAULT_TTL;

  /**
   * KeepAlive Request Stream, put request into this stream to keep the lease alive.
   */
  private StreamObserver<LeaseKeepAliveRequest> keepAliveRequestStreamObserver;

  /**
   * KeepAlive Response Streamer, receive keep alive response from this stream and update the
   * nextKeepAliveTime and deadline of the leases.
   */
  private StreamObserver<LeaseKeepAliveResponse> keepAliveResponseStreamObserver;

  private ExecutorService executorService = Executors.newCachedThreadPool();

  /**
   * Init lease stub.
   */
  public LeaseImpl(final ManagedChannel channel, Optional<String> token) {
    this.channel = channel;
    this.leaseFutureStub = ClientUtil
        .configureStub(LeaseGrpc.newFutureStub(this.channel), token);
    this.leaseStub = ClientUtil.configureStub(LeaseGrpc.newStub(this.channel), token);
    this.scanPeriod = DEFAULT_SCAN_PERIOD;
  }

  /**
   * Init the request stream to etcd
   * start schedule to keep heartbeat to keep alive and remove dead leases.
   *
   * @throws IllegalStateException if the service is running already
   */
  public void startKeepAliveService() {

    /**
     * This function is called by user, and it may be thread unsafe,
     * so we sync here.
     */
    synchronized (this) {
      if (isKeepAliveServiceRunning()) {
        throw new IllegalStateException("KeepAlive keep alive service already started");
      }
      keepAliveResponseStreamObserver = new StreamObserver<LeaseKeepAliveResponse>() {
        @Override
        public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
          processKeepAliveRespond(leaseKeepAliveResponse);
          KeepAlive keepAlive = keepAlives.get(leaseKeepAliveResponse.getID());
          if (keepAlive != null && keepAlive.isContainHandler()) {
            keepAlive.getLeaseHandler().onKeepAliveRespond(leaseKeepAliveResponse);
          }
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
        }
      };

      initRequestStream(keepAliveResponseStreamObserver);

      /**
       * Start heartbeat schedule to keep alive leases and remove dead leases.
       */
      if (this.keepAliveSchedule == null) {
        this.keepAliveSchedule = Executors.newSingleThreadScheduledExecutor();
      }
      this.scheduledFuture = this.keepAliveSchedule.scheduleAtFixedRate(() -> {
        /**
         * The keepAliveExecutor and deadLineExecutor will be sequentially executed in
         * one thread.
         */
        keepAliveExecutor();
        deadLineExecutor();
      }, 0, this.scanPeriod, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * It hints the state of the keep alive service.
   *
   * @return whether the keep alive service is running.
   */
  @Override
  public boolean isKeepAliveServiceRunning() {
    /**
     * This function is called by user, and it may be thread unsafe,
     * so we sync here.
     */
    synchronized (this) {
      return this.scheduledFuture != null && !this.scheduledFuture.isCancelled();
    }
  }

  /**
   * New a lease with ttl value.
   *
   * @param ttl ttl value, unit seconds
   */
  @Override
  public CompletableFuture<LeaseGrantResponse> grant(long ttl) {
    LeaseGrantRequest leaseGrantRequest = LeaseGrantRequest.newBuilder().setTTL(ttl).build();
    return listenableToCompletableFuture(this.leaseFutureStub.leaseGrant(leaseGrantRequest),
        Util::toLeaseGrantResponse, this.executorService);
  }

  /**
   * revoke one lease and the key bind to this lease will be removed.
   *
   * @param leaseId id of the lease to revoke
   */
  @Override
  public CompletableFuture<LeaseRevokeResponse> revoke(long leaseId) {
    LeaseRevokeRequest leaseRevokeRequest = LeaseRevokeRequest.newBuilder().setID(leaseId).build();
    return listenableToCompletableFuture(this.leaseFutureStub.leaseRevoke(leaseRevokeRequest),
        Util::toLeaseRevokeResponse, this.executorService);
  }

  /**
   * keep alive one lease in background, this function is called in
   * user thread, it can be thread unsafe, so we sync function here
   * to avoid put twice.
   *
   * @param leaseId id of lease to set handler
   * @param leaseHandler the handler for the lease, this value can be null
   * @throws IllegalStateException this exception hints that the keep alive service wasn't started
   */
  @Override
  public synchronized void keepAlive(long leaseId, LeaseHandler leaseHandler) {
    if (!isKeepAliveServiceRunning()) {
      throw new IllegalStateException("KeepAlive keep alive service not started yet");
    }
    if (!this.keepAlives.containsKey(leaseId)) {
      KeepAlive keepAlive = new KeepAlive(leaseId, leaseHandler);
      long now = System.currentTimeMillis();
      keepAlive.setNextKeepAlive(now).setDeadLine(now + firstKeepAliveTimeOut);
      this.keepAlives.put(leaseId, keepAlive);
    }
  }

  /**
   * cancel keep alive for lease in background.
   *
   * @param leaseId id of lease
   */
  @Override
  public synchronized void cancelKeepAlive(long leaseId)
      throws ExecutionException, InterruptedException {
    if (!isKeepAliveServiceRunning()) {
      throw new IllegalStateException("KeepAlive keep alive service not started yet");
    }
    if (this.keepAlives.containsKey(leaseId)) {
      keepAlives.remove(leaseId);
      revoke(leaseId).get();
    } else {
      throw new IllegalStateException("KeepAlive is not registered in the keep alive service");
    }
  }

  /**
   * keep alive one lease only once.
   *
   * @param leaseId id of lease to keep alive once
   * @return The keep alive response
   */
  @Override
  public CompletableFuture<com.coreos.jetcd.lease.LeaseKeepAliveResponse> keepAliveOnce(
      long leaseId) {
    CompletableFuture<com.coreos.jetcd.lease.LeaseKeepAliveResponse> lkaFuture =
        new CompletableFuture<>();

    StreamObserver<LeaseKeepAliveRequest> requestObserver = this.leaseStub
        .leaseKeepAlive(new StreamObserver<LeaseKeepAliveResponse>() {
          @Override
          public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
            lkaFuture.complete(toLeaseKeepAliveResponse(leaseKeepAliveResponse));
          }

          @Override
          public void onError(Throwable throwable) {
            lkaFuture.completeExceptionally(throwable);
          }

          @Override
          public void onCompleted() {
          }
        });
    requestObserver.onNext(newKeepAliveRequest(leaseId));

    // cancel grpc stream when leaseKeepAliveResponseCompletableFuture completes.
    lkaFuture
        .whenCompleteAsync((val, throwable) -> requestObserver.onCompleted(), this.executorService);

    return lkaFuture;
  }

  @Override
  public CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId,
      LeaseOption option) {
    checkNotNull(option, "LeaseOption should not be null");

    LeaseTimeToLiveRequest leaseTimeToLiveRequest = LeaseTimeToLiveRequest.newBuilder()
        .setID(leaseId)
        .setKeys(option.isAttachedKeys())
        .build();

    return listenableToCompletableFuture(
        this.leaseFutureStub.leaseTimeToLive(leaseTimeToLiveRequest),
        Util::toLeaseTimeToLiveResponse, this.executorService);
  }

  /**
   * set EtcdLeaseHandler for lease.
   *
   * @param leaseId id of the lease to set handler
   * @param leaseHandler the handler for the lease
   * @throws NoSuchLeaseException if lease do not exist
   */
  @Override
  public void setLeaseHandler(long leaseId, LeaseHandler leaseHandler)
      throws NoSuchLeaseException {
    KeepAlive keepAlive = this.keepAlives.get(leaseId);
    if (keepAlive != null) {
      /**
       * This function may be called with different threads, so
       * we sync here to do it sequentially.
       */
      synchronized (keepAlive) {
        keepAlive.setLeaseHandler(leaseHandler);
      }
    } else {
      throw new NoSuchLeaseException(leaseId);
    }
  }

  /**
   * Scan all the leases and send keep alive request to etcd server
   * This function is called by futureSchedule with one thread, the keepAlives
   * is instance of ConcurrentMap which allow us to do foreach with thread safe,
   * so we have no need to do extra sync.
   */
  private void keepAliveExecutor() {
    long now = System.currentTimeMillis();
    List<Long> toSendIds = new ArrayList<>();
    for (KeepAlive l : this.keepAlives.values()) {
      if (now > l.getNextKeepAlive()) {
        toSendIds.add(l.getLeaseID());
      }
    }

    for (Long id : toSendIds) {
      this.keepAliveRequestStreamObserver.onNext(newKeepAliveRequest(id));
    }
  }

  /**
   * Scan all the leases, remove the dead leases and notify with LeaseHandler
   * This function is called by futureSchedule with one thread, the keepAlives
   * is instance of ConcurrentMap which allow us to do foreach with thread safe,
   * so we have no need to do extra sync.
   */
  private void deadLineExecutor() {
    long now = System.currentTimeMillis();
    List<Long> expireLeases = new ArrayList<>();
    for (KeepAlive l : this.keepAlives.values()) {
      if (now > l.getDeadLine()) {
        expireLeases.add(l.getLeaseID());
      }
    }

    for (Long id : expireLeases) {
      KeepAlive keepAlive = this.keepAlives.get(id);
      if (keepAlive != null && keepAlive.isContainHandler()) {
        keepAlive.getLeaseHandler().onLeaseExpired(id);
      }
      removeLease(id);
    }
  }

  /**
   * This method update the deadline and NextKeepAlive time.
   *
   * @param leaseKeepAliveResponse The response receive from etcd server
   */
  public void processKeepAliveRespond(LeaseKeepAliveResponse leaseKeepAliveResponse) {
    long id = leaseKeepAliveResponse.getID();
    KeepAlive keepAlive = this.keepAlives.get(id);
    if (keepAlive != null) {
      /**
       * This function is called by stream callback from different thread, so
       * we sync here to make the keepAlive set sequentially.
       */
      synchronized (keepAlive) {
        if (leaseKeepAliveResponse.getTTL() <= 0) {
          if (keepAlive != null && keepAlive.isContainHandler()) {
            keepAlive.getLeaseHandler().onLeaseExpired(id);
          }
          removeLease(id);
        } else {
          long nextKeepAlive =
              System.currentTimeMillis() + 1000 + leaseKeepAliveResponse.getTTL() * 1000 / 3;
          keepAlive.setNextKeepAlive(nextKeepAlive);
          keepAlive
              .setDeadLine(System.currentTimeMillis() + leaseKeepAliveResponse.getTTL() * 1000);
        }
      }
    }
  }

  /**
   * remove the lease from keep alive map.
   */
  private void removeLease(long leaseId) {
    if (this.keepAlives.containsKey(leaseId)) {
      this.keepAlives.remove(leaseId);
    }
  }

  private LeaseKeepAliveRequest newKeepAliveRequest(long leaseId) {
    return LeaseKeepAliveRequest.newBuilder().setID(leaseId).build();
  }

  private void initRequestStream(
      StreamObserver<LeaseKeepAliveResponse> leaseKeepAliveResponseStreamObserver) {
    if (this.keepAliveRequestStreamObserver != null) {
      this.keepAliveRequestStreamObserver.onCompleted();
    }
    this.keepAliveRequestStreamObserver = this.leaseStub
        .leaseKeepAlive(leaseKeepAliveResponseStreamObserver);
  }

  /**
   * end the schedule for keep alive and remove dead leases.
   *
   * @throws IllegalStateException if the service is not running yet
   */
  @Override
  public void closeKeepAliveService() {
    /**
     * This function is called by user thread, to make
     * thread safe, we sync here.
     */
    synchronized (this) {
      if (this.scheduledFuture != null) {
        this.keepAliveRequestStreamObserver.onCompleted();
        this.keepAliveRequestStreamObserver = null;
        this.keepAliveResponseStreamObserver = null;
        this.scheduledFuture.cancel(true);
        this.scheduledFuture = null;
      } else {
        throw new IllegalStateException("KeepAlive keep alive service not started yet");
      }
    }
  }
}
