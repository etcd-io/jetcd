package com.coreos.jetcd;

import static com.coreos.jetcd.Util.listenableToCompletableFuture;
import static com.coreos.jetcd.Util.toLeaseKeepAliveResponse;
import static com.google.common.base.Preconditions.checkNotNull;

import com.coreos.jetcd.api.LeaseGrantRequest;
import com.coreos.jetcd.api.LeaseGrpc;
import com.coreos.jetcd.api.LeaseKeepAliveRequest;
import com.coreos.jetcd.api.LeaseRevokeRequest;
import com.coreos.jetcd.api.LeaseTimeToLiveRequest;
import com.coreos.jetcd.lease.KeepAlive;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponseWithError;
import com.coreos.jetcd.lease.LeaseRevokeResponse;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.options.LeaseOption;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of lease client.
 */
public class LeaseImpl implements Lease {

  /**
   * FIRST_KEEPALIVE_TIMEOUT_MS is the timeout for the first keepalive request
   * before the actual TTL is known to the lease client.
   */
  private static final int FIRST_KEEPALIVE_TIMEOUT_MS = 5000;

  private final LeaseGrpc.LeaseFutureStub leaseFutureStub;
  private final LeaseGrpc.LeaseStub leaseStub;

  /**
   * Timer schedule to send keep alive request.
   */
  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
  private ScheduledFuture<?> keepAliveFuture;
  private ScheduledFuture<?> deadlineFuture;

  private final Map<Long, KeepAlive> keepAlives = new ConcurrentHashMap<>();

  /**
   * KeepAlive Request Stream, put request into this stream to keep the lease alive.
   */
  private StreamObserver<LeaseKeepAliveRequest> keepAliveRequestObserver;

  /**
   * KeepAlive Response Streamer, receive keep alive response from this stream and update the
   * nextKeepAliveTime and deadline of the leases.
   */
  private StreamObserver<com.coreos.jetcd.api.LeaseKeepAliveResponse> keepAliveResponseObserver;

  private ExecutorService executorService;

  /**
   * hasKeepAliveServiceStarted indicates whether the background keep alive service has started.
   */
  private boolean hasKeepAliveServiceStarted = false;

  private boolean closed;

  /**
   * Init lease stub with client.
   */
  LeaseImpl(Client c) {
    this(c.getChannel(), c.getToken(), c.getExecutorService());
  }

  LeaseImpl(final ManagedChannel channel, Optional<String> token, ExecutorService executor) {
    this.leaseFutureStub = ClientUtil
        .configureStub(LeaseGrpc.newFutureStub(channel), token);
    this.leaseStub = ClientUtil.configureStub(LeaseGrpc.newStub(channel), token);
    this.executorService = executor;
  }

  @Override
  public CompletableFuture<LeaseGrantResponse> grant(long ttl) {
    LeaseGrantRequest leaseGrantRequest = LeaseGrantRequest.newBuilder().setTTL(ttl).build();
    return listenableToCompletableFuture(this.leaseFutureStub.leaseGrant(leaseGrantRequest),
        Util::toLeaseGrantResponse, this.executorService);
  }

  @Override
  public CompletableFuture<LeaseRevokeResponse> revoke(long leaseId) {
    LeaseRevokeRequest leaseRevokeRequest = LeaseRevokeRequest.newBuilder().setID(leaseId).build();
    return listenableToCompletableFuture(this.leaseFutureStub.leaseRevoke(leaseRevokeRequest),
        Util::toLeaseRevokeResponse, this.executorService);
  }

  @Override
  public synchronized KeepAliveListener keepAlive(long leaseId) {
    if (this.closed) {
      throw new IllegalStateException("Lease client has closed");
    }

    KeepAlive keepAlive = this.keepAlives.computeIfAbsent(leaseId, (key) -> {
      KeepAlive ka = new KeepAlive(this.keepAlives, this, leaseId);
      long now = System.currentTimeMillis();
      ka.setDeadLine(now + FIRST_KEEPALIVE_TIMEOUT_MS);
      ka.setNextKeepAlive(now);
      return ka;
    });

    KeepAliveListenerImpl kal = new KeepAliveListenerImpl(keepAlive);
    keepAlive.addListener(kal);

    if (!this.hasKeepAliveServiceStarted) {
      this.hasKeepAliveServiceStarted = true;
      this.start();
    }

    return kal;
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
    this.closeKeepAlives();
  }

  private synchronized void removeKeepAlive(long leaseId) {
    this.keepAlives.remove(leaseId);
  }

  private void closeKeepAlives() {
    final LeaseKeepAliveResponseWithError errResp = new LeaseKeepAliveResponseWithError(
        new IllegalStateException("Lease client has closed"));
    this.keepAlives.values().forEach(ka -> {
      ka.sentKeepAliveResp(errResp);
      ka.close();
    });
    this.keepAlives.clear();
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
    this.keepAliveResponseObserver = this.createResponseObserver();
    StreamObserver<LeaseKeepAliveRequest> requestStreamObserver = this.leaseStub
        .leaseKeepAlive(this.keepAliveResponseObserver);
    this.keepAliveRequestObserver = requestStreamObserver;
    this.keepAliveFuture = scheduledExecutorService
        .scheduleAtFixedRate(() -> {
          long now = System.currentTimeMillis();

          // send keep alive req to the leases whose next keep alive is before now.
          this.keepAlives.entrySet().stream()
              .filter(entry -> entry.getValue().getNextKeepAlive() < now)
              .map((Entry::getKey))
              .collect(Collectors.toList())
              .forEach(leaseID -> requestStreamObserver
                  .onNext(this.newKeepAliveRequest(leaseID)));
        }, 0, 500, TimeUnit.MILLISECONDS);
  }

  private StreamObserver<com.coreos.jetcd.api.LeaseKeepAliveResponse> createResponseObserver() {
    return new StreamObserver<com.coreos.jetcd.api.LeaseKeepAliveResponse>() {
      @Override
      public void onNext(com.coreos.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse) {
        processKeepAliveResponse(leaseKeepAliveResponse);
      }

      @Override
      public void onError(Throwable throwable) {
        processOnError();
      }

      @Override
      public void onCompleted() {
      }
    };
  }

  private synchronized void processOnError() {
    if (this.closed) {
      return;
    }

    this.scheduledExecutorService.schedule(() -> reset(), 500, TimeUnit.MILLISECONDS);
  }

  private synchronized void processKeepAliveResponse(
      com.coreos.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse) {
    if (this.closed) {
      return;
    }

    long leaseID = leaseKeepAliveResponse.getID();
    long ttl = leaseKeepAliveResponse.getTTL();

    KeepAlive ka = this.keepAlives.get(leaseID);
    if (ka == null) { // return if the corresponding keep alive has closed.
      return;
    }

    if (ttl <= 0) {
      // lease expired; close all keep alive
      this.removeKeepAlive(leaseID);
      ka.sentKeepAliveResp(new LeaseKeepAliveResponseWithError(
          new IllegalStateException("Lease " + leaseID + " expired")));
      return;
    }

    long nextKeepAlive =
        System.currentTimeMillis() + ttl * 1000 / 3;
    ka.setNextKeepAlive(nextKeepAlive);
    ka.setDeadLine(System.currentTimeMillis() + ttl * 1000);
    ka.sentKeepAliveResp(new LeaseKeepAliveResponseWithError(leaseKeepAliveResponse));
  }


  private void deadLineExecutor() {
    this.deadlineFuture = scheduledExecutorService
        .scheduleAtFixedRate(() -> {
          long now = System.currentTimeMillis();

          this.keepAlives.values().removeIf((ka -> {
            if (ka.getDeadLine() < now) {
              ka.close();
              return true;
            }
            return false;
          }));
        }, 0, 1000, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(
      long leaseId) {
    CompletableFuture<LeaseKeepAliveResponse> lkaFuture =
        new CompletableFuture<>();

    StreamObserver<LeaseKeepAliveRequest> requestObserver = this.leaseStub
        .leaseKeepAlive(new StreamObserver<com.coreos.jetcd.api.LeaseKeepAliveResponse>() {
          @Override
          public void onNext(com.coreos.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse) {
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
    requestObserver.onNext(this.newKeepAliveRequest(leaseId));

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

  private LeaseKeepAliveRequest newKeepAliveRequest(long leaseId) {
    return LeaseKeepAliveRequest.newBuilder().setID(leaseId).build();
  }


  public class KeepAliveListenerImpl implements KeepAliveListener {

    private BlockingQueue<LeaseKeepAliveResponseWithError> queue = new LinkedBlockingDeque<>(1);
    private ExecutorService service = Executors.newSingleThreadExecutor();
    private final Object closedLock = new Object();
    private boolean closed = false;
    private Exception reason;
    private KeepAlive owner;

    public KeepAliveListenerImpl(KeepAlive owner) {
      this.owner = owner;
    }

    /**
     * add LeaseKeepAliveResponseWithError to KeepAliveListener's internal queue.
     */
    public void enqueue(LeaseKeepAliveResponseWithError lkae) {
      if (this.isClosed()) {
        return;
      }
      if (lkae.error != null) {
        // returned error to the user on next listen() call.
        this.queue.clear();
      }
      this.queue.offer(lkae);
    }

    @Override
    public synchronized LeaseKeepAliveResponse listen()
        throws InterruptedException {
      if (this.isClosed()) {
        throw new IllegalStateException("KeepAliveListener has closed");
      }

      if (this.reason != null) {
        throw new IllegalStateException(this.reason);
      }

      Future<LeaseKeepAliveResponse> future = service.submit(() -> {
        LeaseKeepAliveResponseWithError lkae = this.queue.take();
        if (lkae.error != null) {
          this.reason = lkae.error;
          throw lkae.error;
        }
        return toLeaseKeepAliveResponse(lkae.leaseKeepAliveResponse);
      });

      try {
        return future.get();
      } catch (ExecutionException e) {
        if (e.getCause() instanceof RejectedExecutionException) {
          throw new IllegalStateException("KeepAliveListener has closed");
        }

        throw new IllegalStateException("KeepAliveListener encounters error on listen",
            e.getCause());
      }
    }

    private boolean isClosed() {
      synchronized (this.closedLock) {
        return this.closed;
      }
    }

    @Override
    public void close() {
      synchronized (this.closedLock) {
        this.closed = true;
        this.owner.removeListener(this);
        this.service.shutdownNow();
      }
    }
  }
}
