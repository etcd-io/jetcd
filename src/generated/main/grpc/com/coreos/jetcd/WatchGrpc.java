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
public class WatchGrpc {

  private WatchGrpc() {}

  public static final String SERVICE_NAME = "jetcd.Watch";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.WatchRequest,
      com.coreos.jetcd.WatchResponse> METHOD_WATCH =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "jetcd.Watch", "Watch"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.WatchRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.WatchResponse.getDefaultInstance()));

  public static WatchStub newStub(io.grpc.Channel channel) {
    return new WatchStub(channel);
  }

  public static WatchBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new WatchBlockingStub(channel);
  }

  public static WatchFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new WatchFutureStub(channel);
  }

  public static interface Watch {

    public io.grpc.stub.StreamObserver<com.coreos.jetcd.WatchRequest> watch(
        io.grpc.stub.StreamObserver<com.coreos.jetcd.WatchResponse> responseObserver);
  }

  public static interface WatchBlockingClient {
  }

  public static interface WatchFutureClient {
  }

  public static class WatchStub extends io.grpc.stub.AbstractStub<WatchStub>
      implements Watch {
    private WatchStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WatchStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WatchStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WatchStub(channel, callOptions);
    }

    @java.lang.Override
    public io.grpc.stub.StreamObserver<com.coreos.jetcd.WatchRequest> watch(
        io.grpc.stub.StreamObserver<com.coreos.jetcd.WatchResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_WATCH, getCallOptions()), responseObserver);
    }
  }

  public static class WatchBlockingStub extends io.grpc.stub.AbstractStub<WatchBlockingStub>
      implements WatchBlockingClient {
    private WatchBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WatchBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WatchBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WatchBlockingStub(channel, callOptions);
    }
  }

  public static class WatchFutureStub extends io.grpc.stub.AbstractStub<WatchFutureStub>
      implements WatchFutureClient {
    private WatchFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WatchFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WatchFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WatchFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_WATCH = 0;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final Watch serviceImpl;
    private final int methodId;

    public MethodHandlers(Watch serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_WATCH:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.watch(
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.WatchResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final Watch serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME)
        .addMethod(
          METHOD_WATCH,
          asyncBidiStreamingCall(
            new MethodHandlers<
              com.coreos.jetcd.WatchRequest,
              com.coreos.jetcd.WatchResponse>(
                serviceImpl, METHODID_WATCH)))
        .build();
  }
}
