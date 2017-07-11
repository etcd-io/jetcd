package com.coreos.jetcd.internal.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.api.AlarmRequest;
import com.coreos.jetcd.api.AlarmType;
import com.coreos.jetcd.api.DefragmentRequest;
import com.coreos.jetcd.api.MaintenanceGrpc;
import com.coreos.jetcd.api.SnapshotRequest;
import com.coreos.jetcd.api.SnapshotResponse;
import com.coreos.jetcd.api.StatusRequest;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.coreos.jetcd.maintenance.AlarmResponse;
import com.coreos.jetcd.maintenance.DefragmentResponse;
import com.coreos.jetcd.maintenance.SnapshotReaderResponseWithError;
import com.coreos.jetcd.maintenance.StatusResponse;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of maintenance client.
 */
class MaintenanceImpl implements Maintenance {

  private final ClientConnectionManager connectionManager;
  private final MaintenanceGrpc.MaintenanceFutureStub stub;
  private final MaintenanceGrpc.MaintenanceStub streamStub;

  MaintenanceImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(MaintenanceGrpc::newFutureStub);
    this.streamStub = connectionManager.newStub(MaintenanceGrpc::newStub);
  }

  /**
   * get all active keyspace alarm.
   *
   * @return alarm list
   */
  @Override
  public CompletableFuture<AlarmResponse> listAlarms() {
    AlarmRequest alarmRequest = AlarmRequest.newBuilder()
        .setAlarm(AlarmType.NONE)
        .setAction(AlarmRequest.AlarmAction.GET)
        .setMemberID(0).build();

    return Util.listenableToCompletableFuture(
        this.stub.alarm(alarmRequest),
        Util::toAlarmResponse,
        this.connectionManager.getExecutorService()
    );
  }

  /**
   * disarms a given alarm.
   *
   * @param member the alarm
   * @return the response result
   */
  @Override
  public CompletableFuture<AlarmResponse> alarmDisarm(
      com.coreos.jetcd.maintenance.AlarmMember member) {
    checkArgument(member.getMemberId() != 0, "the member id can not be 0");
    checkArgument(member.getAlarmType() != com.coreos.jetcd.maintenance.AlarmType.NONE,
        "alarm type can not be NONE");

    AlarmRequest alarmRequest = AlarmRequest.newBuilder()
        .setAlarm(AlarmType.NOSPACE)
        .setAction(AlarmRequest.AlarmAction.DEACTIVATE)
        .setMemberID(member.getMemberId())
        .build();

    return Util.listenableToCompletableFuture(
        this.stub.alarm(alarmRequest),
        Util::toAlarmResponse,
        this.connectionManager.getExecutorService()
    );
  }

  /**
   * defragment one member of the cluster.
   *
   * <p>After compacting the keyspace, the backend database may exhibit internal
   * fragmentation. Any internal fragmentation is space that is free to use
   * by the backend but still consumes storage space. The process of
   * defragmentation releases this storage space back to the file system.
   * Defragmentation is issued on a per-member so that cluster-wide latency
   * spikes may be avoided.
   *
   * <p>Defragment is an expensive operation. User should avoid defragmenting
   * multiple members at the same time.
   * To defragment multiple members in the cluster, user need to call defragment
   * multiple times with different endpoints.
   */
  @Override
  public CompletableFuture<DefragmentResponse> defragmentMember(
      String endpoint) {
    return this.connectionManager.withNewChannel(
        endpoint,
        MaintenanceGrpc::newFutureStub,
        stub -> Util.listenableToCompletableFuture(
            stub.defragment(DefragmentRequest.getDefaultInstance()),
            Util::toDefragmentResponse,
            this.connectionManager.getExecutorService()
        )
    );
  }

  /**
   * get the status of one member.
   */
  @Override
  public CompletableFuture<StatusResponse> statusMember(
      String endpoint) {
    return this.connectionManager.withNewChannel(
        endpoint,
        MaintenanceGrpc::newFutureStub,
        stub -> Util.listenableToCompletableFuture(
            stub.status(StatusRequest.getDefaultInstance()),
            Util::toStatusResponse,
            this.connectionManager.getExecutorService()
        )
    );
  }

  @Override
  public Snapshot snapshot() {
    SnapshotImpl snapshot = new SnapshotImpl();
    this.streamStub.snapshot(SnapshotRequest.getDefaultInstance(), snapshot.getSnapshotObserver());
    return snapshot;
  }

  class SnapshotImpl implements Snapshot {

    private final SnapshotResponse endOfStreamResponse =
        SnapshotResponse.newBuilder().setRemainingBytes(-1).build();
    private StreamObserver<SnapshotResponse> snapshotObserver;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private BlockingQueue<SnapshotReaderResponseWithError> snapshotResponseBlockingQueue =
        new LinkedBlockingQueue<>();

    // closeLock protects closed.
    private final Object closeLock = new Object();
    private boolean closed = false;

    private boolean writeOnce = false;

    SnapshotImpl() {
      this.snapshotObserver = this.createSnapshotObserver();
    }

    private StreamObserver<SnapshotResponse> getSnapshotObserver() {
      return snapshotObserver;
    }

    private StreamObserver<SnapshotResponse> createSnapshotObserver() {
      return new StreamObserver<SnapshotResponse>() {
        @Override
        public void onNext(SnapshotResponse snapshotResponse) {
          snapshotResponseBlockingQueue
              .add(new SnapshotReaderResponseWithError(snapshotResponse));
        }

        @Override
        public void onError(Throwable throwable) {
          snapshotResponseBlockingQueue.add(
              new SnapshotReaderResponseWithError(
                  EtcdExceptionFactory.newConnectException("connection error ", throwable)));
        }

        @Override
        public void onCompleted() {
          snapshotResponseBlockingQueue
              .add(new SnapshotReaderResponseWithError(endOfStreamResponse));
        }
      };
    }

    private boolean isClosed() {
      synchronized (this.closeLock) {
        return this.closed;
      }
    }

    @Override
    public void close() throws IOException {
      synchronized (this.closeLock) {
        if (this.closed) {
          return;
        }
        this.closed = true;
      }

      this.snapshotObserver.onCompleted();
      this.snapshotObserver = null;
      this.snapshotResponseBlockingQueue.clear();
      this.executorService.shutdownNow();
      try {
        this.executorService.awaitTermination(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public synchronized void write(OutputStream os) throws IOException {
      checkNotNull(os);
      if (this.isClosed()) {
        throw new IOException("Snapshot has closed");
      }
      if (this.writeOnce) {
        throw new IOException("write is called more than once");
      }
      this.writeOnce = true;

      Future<Integer> done = this.executorService.submit(() -> {
        while (true) {
          SnapshotReaderResponseWithError snapshotReaderResponseWithError =
              this.snapshotResponseBlockingQueue.take();
          if (snapshotReaderResponseWithError.error != null) {
            throw snapshotReaderResponseWithError.error;
          }

          SnapshotResponse snapshotResponse =
              snapshotReaderResponseWithError.snapshotResponse;
          if (snapshotResponse.getRemainingBytes() == -1) {
            return -1;
          }
          os.write(snapshotResponse.getBlob().toByteArray());
        }
      });

      try {
        done.get();
      } catch (InterruptedException e) {
        throw new IOException("write is interrupted", e);
      } catch (ExecutionException e) {
        throw new IOException(e.getCause());
      } catch (RejectedExecutionException e) {
        throw new IOException("Snapshot has closed");
      }
    }
  }
}
