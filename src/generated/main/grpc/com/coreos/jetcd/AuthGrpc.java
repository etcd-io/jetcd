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
public class AuthGrpc {

  private AuthGrpc() {}

  public static final String SERVICE_NAME = "jetcd.Auth";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthEnableRequest,
      com.coreos.jetcd.AuthEnableResponse> METHOD_AUTH_ENABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "AuthEnable"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthEnableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthEnableResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthDisableRequest,
      com.coreos.jetcd.AuthDisableResponse> METHOD_AUTH_DISABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "AuthDisable"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthDisableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthDisableResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthenticateRequest,
      com.coreos.jetcd.AuthenticateResponse> METHOD_AUTHENTICATE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "Authenticate"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthenticateRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthenticateResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthUserAddRequest,
      com.coreos.jetcd.AuthUserAddResponse> METHOD_USER_ADD =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "UserAdd"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserAddRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserAddResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthUserGetRequest,
      com.coreos.jetcd.AuthUserGetResponse> METHOD_USER_GET =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "UserGet"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserGetRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserGetResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthUserListRequest,
      com.coreos.jetcd.AuthUserListResponse> METHOD_USER_LIST =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "UserList"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserListRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserListResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthUserDeleteRequest,
      com.coreos.jetcd.AuthUserDeleteResponse> METHOD_USER_DELETE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "UserDelete"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserDeleteRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserDeleteResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthUserChangePasswordRequest,
      com.coreos.jetcd.AuthUserChangePasswordResponse> METHOD_USER_CHANGE_PASSWORD =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "UserChangePassword"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserChangePasswordRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserChangePasswordResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthUserGrantRoleRequest,
      com.coreos.jetcd.AuthUserGrantRoleResponse> METHOD_USER_GRANT_ROLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "UserGrantRole"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserGrantRoleRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserGrantRoleResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthUserRevokeRoleRequest,
      com.coreos.jetcd.AuthUserRevokeRoleResponse> METHOD_USER_REVOKE_ROLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "UserRevokeRole"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserRevokeRoleRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthUserRevokeRoleResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthRoleAddRequest,
      com.coreos.jetcd.AuthRoleAddResponse> METHOD_ROLE_ADD =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "RoleAdd"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleAddRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleAddResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthRoleGetRequest,
      com.coreos.jetcd.AuthRoleGetResponse> METHOD_ROLE_GET =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "RoleGet"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleGetRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleGetResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthRoleListRequest,
      com.coreos.jetcd.AuthRoleListResponse> METHOD_ROLE_LIST =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "RoleList"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleListRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleListResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthRoleDeleteRequest,
      com.coreos.jetcd.AuthRoleDeleteResponse> METHOD_ROLE_DELETE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "RoleDelete"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleDeleteRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleDeleteResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthRoleGrantPermissionRequest,
      com.coreos.jetcd.AuthRoleGrantPermissionResponse> METHOD_ROLE_GRANT_PERMISSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "RoleGrantPermission"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleGrantPermissionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleGrantPermissionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<com.coreos.jetcd.AuthRoleRevokePermissionRequest,
      com.coreos.jetcd.AuthRoleRevokePermissionResponse> METHOD_ROLE_REVOKE_PERMISSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "jetcd.Auth", "RoleRevokePermission"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleRevokePermissionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.coreos.jetcd.AuthRoleRevokePermissionResponse.getDefaultInstance()));

  public static AuthStub newStub(io.grpc.Channel channel) {
    return new AuthStub(channel);
  }

  public static AuthBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new AuthBlockingStub(channel);
  }

  public static AuthFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new AuthFutureStub(channel);
  }

  public static interface Auth {

    public void authEnable(com.coreos.jetcd.AuthEnableRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthEnableResponse> responseObserver);

    public void authDisable(com.coreos.jetcd.AuthDisableRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthDisableResponse> responseObserver);

    public void authenticate(com.coreos.jetcd.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthenticateResponse> responseObserver);

    public void userAdd(com.coreos.jetcd.AuthUserAddRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserAddResponse> responseObserver);

    public void userGet(com.coreos.jetcd.AuthUserGetRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserGetResponse> responseObserver);

    public void userList(com.coreos.jetcd.AuthUserListRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserListResponse> responseObserver);

    public void userDelete(com.coreos.jetcd.AuthUserDeleteRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserDeleteResponse> responseObserver);

    public void userChangePassword(com.coreos.jetcd.AuthUserChangePasswordRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserChangePasswordResponse> responseObserver);

    public void userGrantRole(com.coreos.jetcd.AuthUserGrantRoleRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserGrantRoleResponse> responseObserver);

    public void userRevokeRole(com.coreos.jetcd.AuthUserRevokeRoleRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserRevokeRoleResponse> responseObserver);

    public void roleAdd(com.coreos.jetcd.AuthRoleAddRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleAddResponse> responseObserver);

    public void roleGet(com.coreos.jetcd.AuthRoleGetRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleGetResponse> responseObserver);

    public void roleList(com.coreos.jetcd.AuthRoleListRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleListResponse> responseObserver);

    public void roleDelete(com.coreos.jetcd.AuthRoleDeleteRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleDeleteResponse> responseObserver);

    public void roleGrantPermission(com.coreos.jetcd.AuthRoleGrantPermissionRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleGrantPermissionResponse> responseObserver);

    public void roleRevokePermission(com.coreos.jetcd.AuthRoleRevokePermissionRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleRevokePermissionResponse> responseObserver);
  }

  public static interface AuthBlockingClient {

    public com.coreos.jetcd.AuthEnableResponse authEnable(com.coreos.jetcd.AuthEnableRequest request);

    public com.coreos.jetcd.AuthDisableResponse authDisable(com.coreos.jetcd.AuthDisableRequest request);

    public com.coreos.jetcd.AuthenticateResponse authenticate(com.coreos.jetcd.AuthenticateRequest request);

    public com.coreos.jetcd.AuthUserAddResponse userAdd(com.coreos.jetcd.AuthUserAddRequest request);

    public com.coreos.jetcd.AuthUserGetResponse userGet(com.coreos.jetcd.AuthUserGetRequest request);

    public com.coreos.jetcd.AuthUserListResponse userList(com.coreos.jetcd.AuthUserListRequest request);

    public com.coreos.jetcd.AuthUserDeleteResponse userDelete(com.coreos.jetcd.AuthUserDeleteRequest request);

    public com.coreos.jetcd.AuthUserChangePasswordResponse userChangePassword(com.coreos.jetcd.AuthUserChangePasswordRequest request);

    public com.coreos.jetcd.AuthUserGrantRoleResponse userGrantRole(com.coreos.jetcd.AuthUserGrantRoleRequest request);

    public com.coreos.jetcd.AuthUserRevokeRoleResponse userRevokeRole(com.coreos.jetcd.AuthUserRevokeRoleRequest request);

    public com.coreos.jetcd.AuthRoleAddResponse roleAdd(com.coreos.jetcd.AuthRoleAddRequest request);

    public com.coreos.jetcd.AuthRoleGetResponse roleGet(com.coreos.jetcd.AuthRoleGetRequest request);

    public com.coreos.jetcd.AuthRoleListResponse roleList(com.coreos.jetcd.AuthRoleListRequest request);

    public com.coreos.jetcd.AuthRoleDeleteResponse roleDelete(com.coreos.jetcd.AuthRoleDeleteRequest request);

    public com.coreos.jetcd.AuthRoleGrantPermissionResponse roleGrantPermission(com.coreos.jetcd.AuthRoleGrantPermissionRequest request);

    public com.coreos.jetcd.AuthRoleRevokePermissionResponse roleRevokePermission(com.coreos.jetcd.AuthRoleRevokePermissionRequest request);
  }

  public static interface AuthFutureClient {

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthEnableResponse> authEnable(
        com.coreos.jetcd.AuthEnableRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthDisableResponse> authDisable(
        com.coreos.jetcd.AuthDisableRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthenticateResponse> authenticate(
        com.coreos.jetcd.AuthenticateRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserAddResponse> userAdd(
        com.coreos.jetcd.AuthUserAddRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserGetResponse> userGet(
        com.coreos.jetcd.AuthUserGetRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserListResponse> userList(
        com.coreos.jetcd.AuthUserListRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserDeleteResponse> userDelete(
        com.coreos.jetcd.AuthUserDeleteRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserChangePasswordResponse> userChangePassword(
        com.coreos.jetcd.AuthUserChangePasswordRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserGrantRoleResponse> userGrantRole(
        com.coreos.jetcd.AuthUserGrantRoleRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserRevokeRoleResponse> userRevokeRole(
        com.coreos.jetcd.AuthUserRevokeRoleRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleAddResponse> roleAdd(
        com.coreos.jetcd.AuthRoleAddRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleGetResponse> roleGet(
        com.coreos.jetcd.AuthRoleGetRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleListResponse> roleList(
        com.coreos.jetcd.AuthRoleListRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleDeleteResponse> roleDelete(
        com.coreos.jetcd.AuthRoleDeleteRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleGrantPermissionResponse> roleGrantPermission(
        com.coreos.jetcd.AuthRoleGrantPermissionRequest request);

    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleRevokePermissionResponse> roleRevokePermission(
        com.coreos.jetcd.AuthRoleRevokePermissionRequest request);
  }

  public static class AuthStub extends io.grpc.stub.AbstractStub<AuthStub>
      implements Auth {
    private AuthStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuthStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuthStub(channel, callOptions);
    }

    @java.lang.Override
    public void authEnable(com.coreos.jetcd.AuthEnableRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthEnableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_AUTH_ENABLE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void authDisable(com.coreos.jetcd.AuthDisableRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthDisableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_AUTH_DISABLE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void authenticate(com.coreos.jetcd.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthenticateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_AUTHENTICATE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void userAdd(com.coreos.jetcd.AuthUserAddRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserAddResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_USER_ADD, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void userGet(com.coreos.jetcd.AuthUserGetRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserGetResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_USER_GET, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void userList(com.coreos.jetcd.AuthUserListRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserListResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_USER_LIST, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void userDelete(com.coreos.jetcd.AuthUserDeleteRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserDeleteResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_USER_DELETE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void userChangePassword(com.coreos.jetcd.AuthUserChangePasswordRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserChangePasswordResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_USER_CHANGE_PASSWORD, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void userGrantRole(com.coreos.jetcd.AuthUserGrantRoleRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserGrantRoleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_USER_GRANT_ROLE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void userRevokeRole(com.coreos.jetcd.AuthUserRevokeRoleRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserRevokeRoleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_USER_REVOKE_ROLE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void roleAdd(com.coreos.jetcd.AuthRoleAddRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleAddResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ROLE_ADD, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void roleGet(com.coreos.jetcd.AuthRoleGetRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleGetResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ROLE_GET, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void roleList(com.coreos.jetcd.AuthRoleListRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleListResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ROLE_LIST, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void roleDelete(com.coreos.jetcd.AuthRoleDeleteRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleDeleteResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ROLE_DELETE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void roleGrantPermission(com.coreos.jetcd.AuthRoleGrantPermissionRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleGrantPermissionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ROLE_GRANT_PERMISSION, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void roleRevokePermission(com.coreos.jetcd.AuthRoleRevokePermissionRequest request,
        io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleRevokePermissionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ROLE_REVOKE_PERMISSION, getCallOptions()), request, responseObserver);
    }
  }

  public static class AuthBlockingStub extends io.grpc.stub.AbstractStub<AuthBlockingStub>
      implements AuthBlockingClient {
    private AuthBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuthBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuthBlockingStub(channel, callOptions);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthEnableResponse authEnable(com.coreos.jetcd.AuthEnableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_AUTH_ENABLE, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthDisableResponse authDisable(com.coreos.jetcd.AuthDisableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_AUTH_DISABLE, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthenticateResponse authenticate(com.coreos.jetcd.AuthenticateRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_AUTHENTICATE, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthUserAddResponse userAdd(com.coreos.jetcd.AuthUserAddRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_USER_ADD, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthUserGetResponse userGet(com.coreos.jetcd.AuthUserGetRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_USER_GET, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthUserListResponse userList(com.coreos.jetcd.AuthUserListRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_USER_LIST, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthUserDeleteResponse userDelete(com.coreos.jetcd.AuthUserDeleteRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_USER_DELETE, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthUserChangePasswordResponse userChangePassword(com.coreos.jetcd.AuthUserChangePasswordRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_USER_CHANGE_PASSWORD, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthUserGrantRoleResponse userGrantRole(com.coreos.jetcd.AuthUserGrantRoleRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_USER_GRANT_ROLE, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthUserRevokeRoleResponse userRevokeRole(com.coreos.jetcd.AuthUserRevokeRoleRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_USER_REVOKE_ROLE, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthRoleAddResponse roleAdd(com.coreos.jetcd.AuthRoleAddRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ROLE_ADD, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthRoleGetResponse roleGet(com.coreos.jetcd.AuthRoleGetRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ROLE_GET, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthRoleListResponse roleList(com.coreos.jetcd.AuthRoleListRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ROLE_LIST, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthRoleDeleteResponse roleDelete(com.coreos.jetcd.AuthRoleDeleteRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ROLE_DELETE, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthRoleGrantPermissionResponse roleGrantPermission(com.coreos.jetcd.AuthRoleGrantPermissionRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ROLE_GRANT_PERMISSION, getCallOptions(), request);
    }

    @java.lang.Override
    public com.coreos.jetcd.AuthRoleRevokePermissionResponse roleRevokePermission(com.coreos.jetcd.AuthRoleRevokePermissionRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ROLE_REVOKE_PERMISSION, getCallOptions(), request);
    }
  }

  public static class AuthFutureStub extends io.grpc.stub.AbstractStub<AuthFutureStub>
      implements AuthFutureClient {
    private AuthFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuthFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuthFutureStub(channel, callOptions);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthEnableResponse> authEnable(
        com.coreos.jetcd.AuthEnableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_AUTH_ENABLE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthDisableResponse> authDisable(
        com.coreos.jetcd.AuthDisableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_AUTH_DISABLE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthenticateResponse> authenticate(
        com.coreos.jetcd.AuthenticateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_AUTHENTICATE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserAddResponse> userAdd(
        com.coreos.jetcd.AuthUserAddRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_USER_ADD, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserGetResponse> userGet(
        com.coreos.jetcd.AuthUserGetRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_USER_GET, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserListResponse> userList(
        com.coreos.jetcd.AuthUserListRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_USER_LIST, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserDeleteResponse> userDelete(
        com.coreos.jetcd.AuthUserDeleteRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_USER_DELETE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserChangePasswordResponse> userChangePassword(
        com.coreos.jetcd.AuthUserChangePasswordRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_USER_CHANGE_PASSWORD, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserGrantRoleResponse> userGrantRole(
        com.coreos.jetcd.AuthUserGrantRoleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_USER_GRANT_ROLE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthUserRevokeRoleResponse> userRevokeRole(
        com.coreos.jetcd.AuthUserRevokeRoleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_USER_REVOKE_ROLE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleAddResponse> roleAdd(
        com.coreos.jetcd.AuthRoleAddRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ROLE_ADD, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleGetResponse> roleGet(
        com.coreos.jetcd.AuthRoleGetRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ROLE_GET, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleListResponse> roleList(
        com.coreos.jetcd.AuthRoleListRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ROLE_LIST, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleDeleteResponse> roleDelete(
        com.coreos.jetcd.AuthRoleDeleteRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ROLE_DELETE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleGrantPermissionResponse> roleGrantPermission(
        com.coreos.jetcd.AuthRoleGrantPermissionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ROLE_GRANT_PERMISSION, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<com.coreos.jetcd.AuthRoleRevokePermissionResponse> roleRevokePermission(
        com.coreos.jetcd.AuthRoleRevokePermissionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ROLE_REVOKE_PERMISSION, getCallOptions()), request);
    }
  }

  private static final int METHODID_AUTH_ENABLE = 0;
  private static final int METHODID_AUTH_DISABLE = 1;
  private static final int METHODID_AUTHENTICATE = 2;
  private static final int METHODID_USER_ADD = 3;
  private static final int METHODID_USER_GET = 4;
  private static final int METHODID_USER_LIST = 5;
  private static final int METHODID_USER_DELETE = 6;
  private static final int METHODID_USER_CHANGE_PASSWORD = 7;
  private static final int METHODID_USER_GRANT_ROLE = 8;
  private static final int METHODID_USER_REVOKE_ROLE = 9;
  private static final int METHODID_ROLE_ADD = 10;
  private static final int METHODID_ROLE_GET = 11;
  private static final int METHODID_ROLE_LIST = 12;
  private static final int METHODID_ROLE_DELETE = 13;
  private static final int METHODID_ROLE_GRANT_PERMISSION = 14;
  private static final int METHODID_ROLE_REVOKE_PERMISSION = 15;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final Auth serviceImpl;
    private final int methodId;

    public MethodHandlers(Auth serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_AUTH_ENABLE:
          serviceImpl.authEnable((com.coreos.jetcd.AuthEnableRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthEnableResponse>) responseObserver);
          break;
        case METHODID_AUTH_DISABLE:
          serviceImpl.authDisable((com.coreos.jetcd.AuthDisableRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthDisableResponse>) responseObserver);
          break;
        case METHODID_AUTHENTICATE:
          serviceImpl.authenticate((com.coreos.jetcd.AuthenticateRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthenticateResponse>) responseObserver);
          break;
        case METHODID_USER_ADD:
          serviceImpl.userAdd((com.coreos.jetcd.AuthUserAddRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserAddResponse>) responseObserver);
          break;
        case METHODID_USER_GET:
          serviceImpl.userGet((com.coreos.jetcd.AuthUserGetRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserGetResponse>) responseObserver);
          break;
        case METHODID_USER_LIST:
          serviceImpl.userList((com.coreos.jetcd.AuthUserListRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserListResponse>) responseObserver);
          break;
        case METHODID_USER_DELETE:
          serviceImpl.userDelete((com.coreos.jetcd.AuthUserDeleteRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserDeleteResponse>) responseObserver);
          break;
        case METHODID_USER_CHANGE_PASSWORD:
          serviceImpl.userChangePassword((com.coreos.jetcd.AuthUserChangePasswordRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserChangePasswordResponse>) responseObserver);
          break;
        case METHODID_USER_GRANT_ROLE:
          serviceImpl.userGrantRole((com.coreos.jetcd.AuthUserGrantRoleRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserGrantRoleResponse>) responseObserver);
          break;
        case METHODID_USER_REVOKE_ROLE:
          serviceImpl.userRevokeRole((com.coreos.jetcd.AuthUserRevokeRoleRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthUserRevokeRoleResponse>) responseObserver);
          break;
        case METHODID_ROLE_ADD:
          serviceImpl.roleAdd((com.coreos.jetcd.AuthRoleAddRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleAddResponse>) responseObserver);
          break;
        case METHODID_ROLE_GET:
          serviceImpl.roleGet((com.coreos.jetcd.AuthRoleGetRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleGetResponse>) responseObserver);
          break;
        case METHODID_ROLE_LIST:
          serviceImpl.roleList((com.coreos.jetcd.AuthRoleListRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleListResponse>) responseObserver);
          break;
        case METHODID_ROLE_DELETE:
          serviceImpl.roleDelete((com.coreos.jetcd.AuthRoleDeleteRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleDeleteResponse>) responseObserver);
          break;
        case METHODID_ROLE_GRANT_PERMISSION:
          serviceImpl.roleGrantPermission((com.coreos.jetcd.AuthRoleGrantPermissionRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleGrantPermissionResponse>) responseObserver);
          break;
        case METHODID_ROLE_REVOKE_PERMISSION:
          serviceImpl.roleRevokePermission((com.coreos.jetcd.AuthRoleRevokePermissionRequest) request,
              (io.grpc.stub.StreamObserver<com.coreos.jetcd.AuthRoleRevokePermissionResponse>) responseObserver);
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
      final Auth serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME)
        .addMethod(
          METHOD_AUTH_ENABLE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthEnableRequest,
              com.coreos.jetcd.AuthEnableResponse>(
                serviceImpl, METHODID_AUTH_ENABLE)))
        .addMethod(
          METHOD_AUTH_DISABLE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthDisableRequest,
              com.coreos.jetcd.AuthDisableResponse>(
                serviceImpl, METHODID_AUTH_DISABLE)))
        .addMethod(
          METHOD_AUTHENTICATE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthenticateRequest,
              com.coreos.jetcd.AuthenticateResponse>(
                serviceImpl, METHODID_AUTHENTICATE)))
        .addMethod(
          METHOD_USER_ADD,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthUserAddRequest,
              com.coreos.jetcd.AuthUserAddResponse>(
                serviceImpl, METHODID_USER_ADD)))
        .addMethod(
          METHOD_USER_GET,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthUserGetRequest,
              com.coreos.jetcd.AuthUserGetResponse>(
                serviceImpl, METHODID_USER_GET)))
        .addMethod(
          METHOD_USER_LIST,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthUserListRequest,
              com.coreos.jetcd.AuthUserListResponse>(
                serviceImpl, METHODID_USER_LIST)))
        .addMethod(
          METHOD_USER_DELETE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthUserDeleteRequest,
              com.coreos.jetcd.AuthUserDeleteResponse>(
                serviceImpl, METHODID_USER_DELETE)))
        .addMethod(
          METHOD_USER_CHANGE_PASSWORD,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthUserChangePasswordRequest,
              com.coreos.jetcd.AuthUserChangePasswordResponse>(
                serviceImpl, METHODID_USER_CHANGE_PASSWORD)))
        .addMethod(
          METHOD_USER_GRANT_ROLE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthUserGrantRoleRequest,
              com.coreos.jetcd.AuthUserGrantRoleResponse>(
                serviceImpl, METHODID_USER_GRANT_ROLE)))
        .addMethod(
          METHOD_USER_REVOKE_ROLE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthUserRevokeRoleRequest,
              com.coreos.jetcd.AuthUserRevokeRoleResponse>(
                serviceImpl, METHODID_USER_REVOKE_ROLE)))
        .addMethod(
          METHOD_ROLE_ADD,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthRoleAddRequest,
              com.coreos.jetcd.AuthRoleAddResponse>(
                serviceImpl, METHODID_ROLE_ADD)))
        .addMethod(
          METHOD_ROLE_GET,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthRoleGetRequest,
              com.coreos.jetcd.AuthRoleGetResponse>(
                serviceImpl, METHODID_ROLE_GET)))
        .addMethod(
          METHOD_ROLE_LIST,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthRoleListRequest,
              com.coreos.jetcd.AuthRoleListResponse>(
                serviceImpl, METHODID_ROLE_LIST)))
        .addMethod(
          METHOD_ROLE_DELETE,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthRoleDeleteRequest,
              com.coreos.jetcd.AuthRoleDeleteResponse>(
                serviceImpl, METHODID_ROLE_DELETE)))
        .addMethod(
          METHOD_ROLE_GRANT_PERMISSION,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthRoleGrantPermissionRequest,
              com.coreos.jetcd.AuthRoleGrantPermissionResponse>(
                serviceImpl, METHODID_ROLE_GRANT_PERMISSION)))
        .addMethod(
          METHOD_ROLE_REVOKE_PERMISSION,
          asyncUnaryCall(
            new MethodHandlers<
              com.coreos.jetcd.AuthRoleRevokePermissionRequest,
              com.coreos.jetcd.AuthRoleRevokePermissionResponse>(
                serviceImpl, METHODID_ROLE_REVOKE_PERMISSION)))
        .build();
  }
}
