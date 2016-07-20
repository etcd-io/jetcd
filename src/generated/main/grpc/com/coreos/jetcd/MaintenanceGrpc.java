package com.coreos.jetcd;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@javax.annotation.Generated("by gRPC proto compiler")
public class MaintenanceGrpc {

  private MaintenanceGrpc() {}

  public static final String SERVICE_NAME = "jetcd.Maintenance";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AlarmRequest,
      com.coreos.jetcd.AlarmResponse> METHOD_ALARM =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Maintenance", "Alarm"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AlarmRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AlarmResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.StatusRequest,
      com.coreos.jetcd.StatusResponse> METHOD_STATUS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Maintenance", "Status"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.StatusRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.StatusResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.DefragmentRequest,
      com.coreos.jetcd.DefragmentResponse> METHOD_DEFRAGMENT =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Maintenance", "Defragment"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.DefragmentRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.DefragmentResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.HashRequest,
      com.coreos.jetcd.HashResponse> METHOD_HASH =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Maintenance", "Hash"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.HashRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.HashResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.SnapshotRequest,
      com.coreos.jetcd.SnapshotResponse> METHOD_SNAPSHOT =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
          generateFullMethodName(
              "jetcd.Maintenance", "Snapshot"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.SnapshotRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.SnapshotResponse.getDefaultInstance()));

  public static MaintenanceStub newStub(io.grpc.Channel channel) {
    return new MaintenanceStub(channel);
  }

  public static MaintenanceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new MaintenanceBlockingStub(channel);
  }

  public static MaintenanceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new MaintenanceFutureStub(channel);
  }

  public static interface Maintenance {

    public void alarm(com.coreos.jetcd.AlarmRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AlarmResponse> responseObserver);

    public void status(com.coreos.jetcd.StatusRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.StatusResponse> responseObserver);

    public void defragment(com.coreos.jetcd.DefragmentRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.DefragmentResponse> responseObserver);

    public void hash(com.coreos.jetcd.HashRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.HashResponse> responseObserver);

    public void snapshot(com.coreos.jetcd.SnapshotRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.SnapshotResponse> responseObserver);
  }

  public static interface MaintenanceBlockingClient {

    public com.coreos.jetcd.AlarmResponse alarm(com.coreos.jetcd.AlarmRequest request);

    public com.coreos.jetcd.StatusResponse status(com.coreos.jetcd.StatusRequest request);

    public com.coreos.jetcd.DefragmentResponse defragment(com.coreos.jetcd.DefragmentRequest request);

    public com.coreos.jetcd.HashResponse hash(com.coreos.jetcd.HashRequest request);

    public java.util.Iterator<com.coreos.jetcd.SnapshotResponse> snapshot(
        com.coreos.jetcd.SnapshotRequest request);
  }

  public static interface MaintenanceFutureClient {

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AlarmResponse> alarm(
        com.coreos.jetcd.AlarmRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.StatusResponse> status(
        com.coreos.jetcd.StatusRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.DefragmentResponse> defragment(
        com.coreos.jetcd.DefragmentRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.HashResponse> hash(
        com.coreos.jetcd.HashRequest request);
  }

  public static class MaintenanceStub extends io.grpc.stub.AbstractStub<MaintenanceStub>
      implements Maintenance {
    private MaintenanceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MaintenanceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MaintenanceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MaintenanceStub(channel, callOptions);
    }

    @java.lang.Override
    public void alarm(com.coreos.jetcd.AlarmRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AlarmResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ALARM, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void status(com.coreos.jetcd.StatusRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.StatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_STATUS, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void defragment(com.coreos.jetcd.DefragmentRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.DefragmentResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DEFRAGMENT, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void hash(com.coreos.jetcd.HashRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.HashResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_HASH, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void snapshot(com.coreos.jetcd.SnapshotRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.SnapshotResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_SNAPSHOT, getCallOptions()), request, responseObserver);
    }
  }

  public static class MaintenanceBlockingStub extends io.grpc.stub.AbstractStub<MaintenanceBlockingStub>
      implements MaintenanceBlockingClient {
    private MaintenanceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MaintenanceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MaintenanceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MaintenanceBlockingStub(channel, callOptions);
    }

    @java.lang.Override
    public com.coreos.jetcd.AlarmResponse alarm(com.coreos.jetcd.AlarmRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ALARM, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.StatusResponse status(com.coreos.jetcd.StatusRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_STATUS, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.DefragmentResponse defragment(com.coreos.jetcd.DefragmentRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DEFRAGMENT, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.HashResponse hash(com.coreos.jetcd.HashRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_HASH, getCallOptions(), request);
    }

    @java.lang.Override
    public java.util.Iterator<com.coreos.jetcd.SnapshotResponse> snapshot(
        com.coreos.jetcd.SnapshotRequest request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_SNAPSHOT, getCallOptions(), request);
    }
  }

  public static class MaintenanceFutureStub extends io.grpc.stub.AbstractStub<MaintenanceFutureStub>
      implements MaintenanceFutureClient {
    private MaintenanceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MaintenanceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MaintenanceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MaintenanceFutureStub(channel, callOptions);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AlarmResponse> alarm(
        com.coreos.jetcd.AlarmRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ALARM, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.StatusResponse> status(
        com.coreos.jetcd.StatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_STATUS, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.DefragmentResponse> defragment(
        com.coreos.jetcd.DefragmentRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DEFRAGMENT, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.HashResponse> hash(
        com.coreos.jetcd.HashRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_HASH, getCallOptions()), request);
    }
  }

  private static final int METHODID_ALARM = 0;
  private static final int METHODID_STATUS = 1;
  private static final int METHODID_DEFRAGMENT = 2;
  private static final int METHODID_HASH = 3;
  private static final int METHODID_SNAPSHOT = 4;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final Maintenance serviceImpl;
    private final int methodId;

    public MethodHandlers(Maintenance serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ALARM:
          serviceImpl.alarm((com.coreos.jetcd.AlarmRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AlarmResponse>) responseObserver);
          break;
        case METHODID_STATUS:
          serviceImpl.status((com.coreos.jetcd.StatusRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.StatusResponse>) responseObserver);
          break;
        case METHODID_DEFRAGMENT:
          serviceImpl.defragment((com.coreos.jetcd.DefragmentRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.DefragmentResponse>) responseObserver);
          break;
        case METHODID_HASH:
          serviceImpl.hash((com.coreos.jetcd.HashRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.HashResponse>) responseObserver);
          break;
        case METHODID_SNAPSHOT:
          serviceImpl.snapshot((com.coreos.jetcd.SnapshotRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.SnapshotResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final Maintenance serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME)
        .addMethod(
          METHOD_ALARM,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AlarmRequest,
              com.coreos.jetcd.AlarmResponse>(
                serviceImpl, METHODID_ALARM)))
        .addMethod(
          METHOD_STATUS,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.StatusRequest,
              com.coreos.jetcd.StatusResponse>(
                serviceImpl, METHODID_STATUS)))
        .addMethod(
          METHOD_DEFRAGMENT,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.DefragmentRequest,
              com.coreos.jetcd.DefragmentResponse>(
                serviceImpl, METHODID_DEFRAGMENT)))
        .addMethod(
          METHOD_HASH,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.HashRequest,
              com.coreos.jetcd.HashResponse>(
                serviceImpl, METHODID_HASH)))
        .addMethod(
          METHOD_SNAPSHOT,
          asyncServerStreamingCall(
            new MethodHandlers<
              com.coreos.jetcd.SnapshotRequest,
              com.coreos.jetcd.SnapshotResponse>(
                serviceImpl, METHODID_SNAPSHOT)))
        .build();
  }
}
