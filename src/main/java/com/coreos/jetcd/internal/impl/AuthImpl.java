package com.coreos.jetcd.internal.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.api.AuthDisableRequest;
import com.coreos.jetcd.api.AuthEnableRequest;
import com.coreos.jetcd.api.AuthGrpc;
import com.coreos.jetcd.api.AuthRoleAddRequest;
import com.coreos.jetcd.api.AuthRoleDeleteRequest;
import com.coreos.jetcd.api.AuthRoleGetRequest;
import com.coreos.jetcd.api.AuthRoleGrantPermissionRequest;
import com.coreos.jetcd.api.AuthRoleListRequest;
import com.coreos.jetcd.api.AuthRoleRevokePermissionRequest;
import com.coreos.jetcd.api.AuthUserAddRequest;
import com.coreos.jetcd.api.AuthUserChangePasswordRequest;
import com.coreos.jetcd.api.AuthUserDeleteRequest;
import com.coreos.jetcd.api.AuthUserGetRequest;
import com.coreos.jetcd.api.AuthUserGrantRoleRequest;
import com.coreos.jetcd.api.AuthUserListRequest;
import com.coreos.jetcd.api.AuthUserRevokeRoleRequest;
import com.coreos.jetcd.api.Permission.Type;
import com.coreos.jetcd.auth.AuthDisableResponse;
import com.coreos.jetcd.auth.AuthEnableResponse;
import com.coreos.jetcd.auth.AuthRoleAddResponse;
import com.coreos.jetcd.auth.AuthRoleDeleteResponse;
import com.coreos.jetcd.auth.AuthRoleGetResponse;
import com.coreos.jetcd.auth.AuthRoleGrantPermissionResponse;
import com.coreos.jetcd.auth.AuthRoleListResponse;
import com.coreos.jetcd.auth.AuthRoleRevokePermissionResponse;
import com.coreos.jetcd.auth.AuthUserAddResponse;
import com.coreos.jetcd.auth.AuthUserChangePasswordResponse;
import com.coreos.jetcd.auth.AuthUserDeleteResponse;
import com.coreos.jetcd.auth.AuthUserGetResponse;
import com.coreos.jetcd.auth.AuthUserGrantRoleResponse;
import com.coreos.jetcd.auth.AuthUserListResponse;
import com.coreos.jetcd.auth.AuthUserRevokeRoleResponse;
import com.coreos.jetcd.auth.Permission;
import com.coreos.jetcd.data.ByteSequence;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of etcd auth client.
 */
class AuthImpl implements Auth {

  private final AuthGrpc.AuthFutureStub stub;
  private final ClientConnectionManager connectionManager;

  AuthImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(AuthGrpc::newFutureStub);
  }

  @Override
  public CompletableFuture<AuthEnableResponse> authEnable() {
    AuthEnableRequest enableRequest = AuthEnableRequest.getDefaultInstance();
    return Util.toCompletableFuture(
        this.stub.authEnable(enableRequest),
        AuthEnableResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthDisableResponse> authDisable() {
    AuthDisableRequest disableRequest = AuthDisableRequest.getDefaultInstance();
    return Util.toCompletableFuture(
        this.stub.authDisable(disableRequest),
        AuthDisableResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence user, ByteSequence password) {
    checkNotNull(user, "user can't be null");
    checkNotNull(password, "password can't be null");

    AuthUserAddRequest addRequest = AuthUserAddRequest.newBuilder()
        .setNameBytes(Util.byteStringFromByteSequence(user))
        .setPasswordBytes(Util.byteStringFromByteSequence(password))
        .build();
    return Util.toCompletableFuture(
        this.stub.userAdd(addRequest),
        AuthUserAddResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence user) {
    checkNotNull(user, "user can't be null");

    AuthUserDeleteRequest deleteRequest = AuthUserDeleteRequest.newBuilder()
        .setNameBytes(Util.byteStringFromByteSequence(user))
        .build();
    return Util.toCompletableFuture(
        this.stub.userDelete(deleteRequest),
        AuthUserDeleteResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence user,
      ByteSequence password) {
    checkNotNull(user, "user can't be null");
    checkNotNull(password, "password can't be null");

    AuthUserChangePasswordRequest changePasswordRequest = AuthUserChangePasswordRequest.newBuilder()
        .setNameBytes(Util.byteStringFromByteSequence(user))
        .setPasswordBytes(Util.byteStringFromByteSequence(password))
        .build();
    return Util.toCompletableFuture(
        this.stub.userChangePassword(changePasswordRequest),
        AuthUserChangePasswordResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthUserGetResponse> userGet(ByteSequence user) {
    checkNotNull(user, "user can't be null");

    AuthUserGetRequest userGetRequest = AuthUserGetRequest.newBuilder()
        .setNameBytes(Util.byteStringFromByteSequence(user))
        .build();
    return Util.toCompletableFuture(
        this.stub.userGet(userGetRequest),
        AuthUserGetResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthUserListResponse> userList() {
    AuthUserListRequest userListRequest = AuthUserListRequest.getDefaultInstance();
    return Util.toCompletableFuture(
        this.stub.userList(userListRequest),
        AuthUserListResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence user,
      ByteSequence role) {
    checkNotNull(user, "user can't be null");
    checkNotNull(role, "key can't be null");

    AuthUserGrantRoleRequest userGrantRoleRequest = AuthUserGrantRoleRequest.newBuilder()
        .setUserBytes(Util.byteStringFromByteSequence(user))
        .setRoleBytes(Util.byteStringFromByteSequence(role))
        .build();
    return Util.toCompletableFuture(
        this.stub.userGrantRole(userGrantRoleRequest),
        AuthUserGrantRoleResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence user,
      ByteSequence role) {
    checkNotNull(user, "user can't be null");
    checkNotNull(role, "key can't be null");

    AuthUserRevokeRoleRequest userRevokeRoleRequest = AuthUserRevokeRoleRequest.newBuilder()
        .setNameBytes(Util.byteStringFromByteSequence(user))
        .setRoleBytes(Util.byteStringFromByteSequence(role))
        .build();
    return Util.toCompletableFuture(
        this.stub.userRevokeRole(userRevokeRoleRequest),
        AuthUserRevokeRoleResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence user) {
    checkNotNull(user, "user can't be null");

    AuthRoleAddRequest roleAddRequest = AuthRoleAddRequest.newBuilder()
        .setNameBytes(Util.byteStringFromByteSequence(user))
        .build();
    return Util.toCompletableFuture(
        this.stub.roleAdd(roleAddRequest),
        AuthRoleAddResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteSequence role,
      ByteSequence key, ByteSequence rangeEnd, Permission.Type permType) {
    checkNotNull(role, "role can't be null");
    checkNotNull(key, "key can't be null");
    checkNotNull(rangeEnd, "rangeEnd can't be null");
    checkNotNull(permType, "permType can't be null");

    com.coreos.jetcd.api.Permission.Type type;
    switch (permType) {
      case WRITE:
        type = Type.WRITE;
        break;
      case READWRITE:
        type = Type.READWRITE;
        break;
      case READ:
        type = Type.READ;
        break;
      default:
        type = Type.UNRECOGNIZED;
        break;
    }

    com.coreos.jetcd.api.Permission perm = com.coreos.jetcd.api.Permission.newBuilder()
        .setKey(Util.byteStringFromByteSequence(key))
        .setRangeEnd(Util.byteStringFromByteSequence(rangeEnd))
        .setPermType(type)
        .build();
    AuthRoleGrantPermissionRequest roleGrantPermissionRequest = AuthRoleGrantPermissionRequest
        .newBuilder()
        .setNameBytes(Util.byteStringFromByteSequence(role))
        .setPerm(perm)
        .build();
    return Util.toCompletableFuture(
        this.stub.roleGrantPermission(roleGrantPermissionRequest),
        AuthRoleGrantPermissionResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role) {
    checkNotNull(role, "role can't be null");

    AuthRoleGetRequest roleGetRequest = AuthRoleGetRequest.newBuilder()
        .setRoleBytes(Util.byteStringFromByteSequence(role))
        .build();
    return Util.toCompletableFuture(
        this.stub.roleGet(roleGetRequest),
        AuthRoleGetResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthRoleListResponse> roleList() {
    AuthRoleListRequest roleListRequest = AuthRoleListRequest.getDefaultInstance();
    return Util.toCompletableFuture(
        this.stub.roleList(roleListRequest),
        AuthRoleListResponse::new,
        this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role,
      ByteSequence key, ByteSequence rangeEnd) {
    checkNotNull(role, "role can't be null");
    checkNotNull(key, "key can't be null");
    checkNotNull(rangeEnd, "rangeEnd can't be null");

    AuthRoleRevokePermissionRequest roleRevokePermissionRequest = AuthRoleRevokePermissionRequest
        .newBuilder()
        .setRoleBytes(Util.byteStringFromByteSequence(role))
        .setKeyBytes(Util.byteStringFromByteSequence(key))
        .setRangeEndBytes(Util.byteStringFromByteSequence(rangeEnd))
        .build();
    return Util
        .toCompletableFuture(
            this.stub.roleRevokePermission(roleRevokePermissionRequest),
            AuthRoleRevokePermissionResponse::new,
            this.connectionManager.getExecutorService());
  }

  @Override
  public CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role) {
    checkNotNull(role, "role can't be null");
    AuthRoleDeleteRequest roleDeleteRequest = AuthRoleDeleteRequest.newBuilder()
        .setRoleBytes(Util.byteStringFromByteSequence(role))
        .build();
    return Util.toCompletableFuture(
        this.stub.roleDelete(roleDeleteRequest),
        AuthRoleDeleteResponse::new,
        this.connectionManager.getExecutorService());
  }
}
