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
public class LeaseGrpc {

  private LeaseGrpc() {}

  public static final String SERVICE_NAME = "jetcd.Lease";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.LeaseGrantRequest,
      com.coreos.jetcd.LeaseGrantResponse> METHOD_LEASE_GRANT =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Lease", "LeaseGrant"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.LeaseGrantRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.LeaseGrantResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.LeaseRevokeRequest,
      com.coreos.jetcd.LeaseRevokeResponse> METHOD_LEASE_REVOKE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Lease", "LeaseRevoke"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.LeaseRevokeRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.LeaseRevokeResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.LeaseKeepAliveRequest,
      com.coreos.jetcd.LeaseKeepAliveResponse> METHOD_LEASE_KEEP_ALIVE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "jetcd.Lease", "LeaseKeepAlive"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.LeaseKeepAliveRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.LeaseKeepAliveResponse.getDefaultInstance()));

  public static LeaseStub newStub(io.grpc.Channel channel) {
    return new LeaseStub(channel);
  }

  public static LeaseBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new LeaseBlockingStub(channel);
  }

  public static LeaseFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new LeaseFutureStub(channel);
  }

  public static interface Lease {

    public void leaseGrant(com.coreos.jetcd.LeaseGrantRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseGrantResponse> responseObserver);

    public void leaseRevoke(com.coreos.jetcd.LeaseRevokeRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseRevokeResponse> responseObserver);

    public io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseKeepAliveRequest> leaseKeepAlive(
        io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseKeepAliveResponse> responseObserver);
  }

  public static interface LeaseBlockingClient {

    public com.coreos.jetcd.LeaseGrantResponse leaseGrant(com.coreos.jetcd.LeaseGrantRequest request);

    public com.coreos.jetcd.LeaseRevokeResponse leaseRevoke(com.coreos.jetcd.LeaseRevokeRequest request);
  }

  public static interface LeaseFutureClient {

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.LeaseGrantResponse> leaseGrant(
        com.coreos.jetcd.LeaseGrantRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.LeaseRevokeResponse> leaseRevoke(
        com.coreos.jetcd.LeaseRevokeRequest request);
  }

  public static class LeaseStub extends io.grpc.stub.AbstractStub<LeaseStub>
      implements Lease {
    private LeaseStub(io.grpc.Channel channel) {
      super(channel);
    }

    private LeaseStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeaseStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new LeaseStub(channel, callOptions);
    }

    @java.lang.Override
    public void leaseGrant(com.coreos.jetcd.LeaseGrantRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseGrantResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LEASE_GRANT, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void leaseRevoke(com.coreos.jetcd.LeaseRevokeRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseRevokeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LEASE_REVOKE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseKeepAliveRequest> leaseKeepAlive(
        io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseKeepAliveResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_LEASE_KEEP_ALIVE, getCallOptions()), responseObserver);
    }
  }

  public static class LeaseBlockingStub extends io.grpc.stub.AbstractStub<LeaseBlockingStub>
      implements LeaseBlockingClient {
    private LeaseBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private LeaseBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeaseBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new LeaseBlockingStub(channel, callOptions);
    }

    @java.lang.Override
    public com.coreos.jetcd.LeaseGrantResponse leaseGrant(com.coreos.jetcd.LeaseGrantRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LEASE_GRANT, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.LeaseRevokeResponse leaseRevoke(com.coreos.jetcd.LeaseRevokeRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LEASE_REVOKE, getCallOptions(), request);
    }
  }

  public static class LeaseFutureStub extends io.grpc.stub.AbstractStub<LeaseFutureStub>
      implements LeaseFutureClient {
    private LeaseFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private LeaseFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeaseFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new LeaseFutureStub(channel, callOptions);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.LeaseGrantResponse> leaseGrant(
        com.coreos.jetcd.LeaseGrantRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LEASE_GRANT, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.LeaseRevokeResponse> leaseRevoke(
        com.coreos.jetcd.LeaseRevokeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LEASE_REVOKE, getCallOptions()), request);
    }
  }

  private static final int METHODID_LEASE_GRANT = 0;
  private static final int METHODID_LEASE_REVOKE = 1;
  private static final int METHODID_LEASE_KEEP_ALIVE = 2;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final Lease serviceImpl;
    private final int methodId;

    public MethodHandlers(Lease serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LEASE_GRANT:
          serviceImpl.leaseGrant((com.coreos.jetcd.LeaseGrantRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseGrantResponse>) responseObserver);
          break;
        case METHODID_LEASE_REVOKE:
          serviceImpl.leaseRevoke((com.coreos.jetcd.LeaseRevokeRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseRevokeResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LEASE_KEEP_ALIVE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.leaseKeepAlive(
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.LeaseKeepAliveResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final Lease serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME)
        .addMethod(
          METHOD_LEASE_GRANT,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.LeaseGrantRequest,
              com.coreos.jetcd.LeaseGrantResponse>(
                serviceImpl, METHODID_LEASE_GRANT)))
        .addMethod(
          METHOD_LEASE_REVOKE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.LeaseRevokeRequest,
              com.coreos.jetcd.LeaseRevokeResponse>(
                serviceImpl, METHODID_LEASE_REVOKE)))
        .addMethod(
          METHOD_LEASE_KEEP_ALIVE,
          asyncBidiStreamingCall(
            new MethodHandlers<
              com.coreos.jetcd.LeaseKeepAliveRequest,
              com.coreos.jetcd.LeaseKeepAliveResponse>(
                serviceImpl, METHODID_LEASE_KEEP_ALIVE)))
        .build();
  }
}
