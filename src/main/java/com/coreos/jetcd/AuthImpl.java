package com.coreos.jetcd;

import static com.coreos.jetcd.Util.byteStringFromByteSequence;

import com.coreos.jetcd.api.AuthDisableRequest;
import com.coreos.jetcd.api.AuthDisableResponse;
import com.coreos.jetcd.api.AuthEnableRequest;
import com.coreos.jetcd.api.AuthEnableResponse;
import com.coreos.jetcd.api.AuthGrpc;
import com.coreos.jetcd.api.AuthRoleAddRequest;
import com.coreos.jetcd.api.AuthRoleAddResponse;
import com.coreos.jetcd.api.AuthRoleDeleteRequest;
import com.coreos.jetcd.api.AuthRoleDeleteResponse;
import com.coreos.jetcd.api.AuthRoleGetRequest;
import com.coreos.jetcd.api.AuthRoleGetResponse;
import com.coreos.jetcd.api.AuthRoleGrantPermissionRequest;
import com.coreos.jetcd.api.AuthRoleGrantPermissionResponse;
import com.coreos.jetcd.api.AuthRoleListRequest;
import com.coreos.jetcd.api.AuthRoleListResponse;
import com.coreos.jetcd.api.AuthRoleRevokePermissionRequest;
import com.coreos.jetcd.api.AuthRoleRevokePermissionResponse;
import com.coreos.jetcd.api.AuthUserAddRequest;
import com.coreos.jetcd.api.AuthUserAddResponse;
import com.coreos.jetcd.api.AuthUserChangePasswordRequest;
import com.coreos.jetcd.api.AuthUserChangePasswordResponse;
import com.coreos.jetcd.api.AuthUserDeleteRequest;
import com.coreos.jetcd.api.AuthUserDeleteResponse;
import com.coreos.jetcd.api.AuthUserGetRequest;
import com.coreos.jetcd.api.AuthUserGetResponse;
import com.coreos.jetcd.api.AuthUserGrantRoleRequest;
import com.coreos.jetcd.api.AuthUserGrantRoleResponse;
import com.coreos.jetcd.api.AuthUserListRequest;
import com.coreos.jetcd.api.AuthUserListResponse;
import com.coreos.jetcd.api.AuthUserRevokeRoleRequest;
import com.coreos.jetcd.api.AuthUserRevokeRoleResponse;
import com.coreos.jetcd.api.Permission;
import com.coreos.jetcd.data.ByteSequence;
import io.grpc.ManagedChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

/**
 * Implementation of etcd auth client.
 */
public class AuthImpl implements Auth {

  private final AuthGrpc.AuthFutureStub stub;

  public AuthImpl(ManagedChannel channel, Optional<String> token) {
    this.stub = ClientUtil.configureStub(AuthGrpc.newFutureStub(channel), token);
  }

  // ***************
  // Auth Manage
  // ***************

  @Override
  public CompletableFuture<AuthEnableResponse> authEnable() {
    AuthEnableRequest enableRequest = AuthEnableRequest.getDefaultInstance();
    return FutureConverter.toCompletableFuture(this.stub.authEnable(enableRequest));
  }

  @Override
  public CompletableFuture<AuthDisableResponse> authDisable() {
    AuthDisableRequest disableRequest = AuthDisableRequest.getDefaultInstance();
    return FutureConverter.toCompletableFuture(this.stub.authDisable(disableRequest));
  }

  // ***************
  // User Manage
  // ***************

  @Override
  public CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence name, ByteSequence password) {
    AuthUserAddRequest addRequest = AuthUserAddRequest.newBuilder()
        .setNameBytes(byteStringFromByteSequence(name))
        .setPasswordBytes(byteStringFromByteSequence(password))
        .build();
    return FutureConverter.toCompletableFuture(this.stub.userAdd(addRequest));
  }

  @Override
  public CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence name) {
    AuthUserDeleteRequest deleteRequest = AuthUserDeleteRequest.newBuilder()
        .setNameBytes(byteStringFromByteSequence(name)).build();
    return FutureConverter.toCompletableFuture(this.stub.userDelete(deleteRequest));
  }

  @Override
  public CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence name,
      ByteSequence password) {
    AuthUserChangePasswordRequest changePasswordRequest = AuthUserChangePasswordRequest.newBuilder()
        .setNameBytes(byteStringFromByteSequence(name))
        .setPasswordBytes(byteStringFromByteSequence(password))
        .build();
    return FutureConverter.toCompletableFuture(this.stub.userChangePassword(changePasswordRequest));
  }

  @Override
  public CompletableFuture<AuthUserGetResponse> userGet(ByteSequence name) {
    AuthUserGetRequest userGetRequest = AuthUserGetRequest.newBuilder()
        .setNameBytes(byteStringFromByteSequence(name))
        .build();

    return FutureConverter.toCompletableFuture(this.stub.userGet(userGetRequest));
  }

  @Override
  public CompletableFuture<AuthUserListResponse> userList() {
    AuthUserListRequest userListRequest = AuthUserListRequest.getDefaultInstance();
    return FutureConverter.toCompletableFuture(this.stub.userList(userListRequest));
  }

  // ***************
  // User Role Manage
  // ***************

  @Override
  public CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence name,
      ByteSequence role) {
    AuthUserGrantRoleRequest userGrantRoleRequest = AuthUserGrantRoleRequest.newBuilder()
        .setUserBytes(byteStringFromByteSequence(name))
        .setRoleBytes(byteStringFromByteSequence(role))
        .build();
    return FutureConverter.toCompletableFuture(this.stub.userGrantRole(userGrantRoleRequest));
  }

  @Override
  public CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence name,
      ByteSequence role) {
    AuthUserRevokeRoleRequest userRevokeRoleRequest = AuthUserRevokeRoleRequest.newBuilder()
        .setNameBytes(byteStringFromByteSequence(name))
        .setRoleBytes(byteStringFromByteSequence(role))
        .build();
    return FutureConverter.toCompletableFuture(this.stub.userRevokeRole(userRevokeRoleRequest));
  }

  // ***************
  // Role Manage
  // ***************

  @Override
  public CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence name) {
    AuthRoleAddRequest roleAddRequest = AuthRoleAddRequest.newBuilder()
        .setNameBytes(byteStringFromByteSequence(name))
        .build();
    return FutureConverter.toCompletableFuture(this.stub.roleAdd(roleAddRequest));
  }

  @Override
  public CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteSequence role,
      ByteSequence key, ByteSequence rangeEnd, Permission.Type permType) {
    Permission perm = Permission.newBuilder()
        .setKey(byteStringFromByteSequence(key))
        .setRangeEnd(byteStringFromByteSequence(rangeEnd))
        .setPermType(permType)
        .build();
    AuthRoleGrantPermissionRequest roleGrantPermissionRequest = AuthRoleGrantPermissionRequest
        .newBuilder()
        .setNameBytes(byteStringFromByteSequence(role))
        .setPerm(perm)
        .build();

    return FutureConverter
        .toCompletableFuture(this.stub.roleGrantPermission(roleGrantPermissionRequest));
  }

  @Override
  public CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role) {
    AuthRoleGetRequest roleGetRequest = AuthRoleGetRequest.newBuilder()
        .setRoleBytes(byteStringFromByteSequence(role))
        .build();
    return FutureConverter.toCompletableFuture(this.stub.roleGet(roleGetRequest));
  }

  @Override
  public CompletableFuture<AuthRoleListResponse> roleList() {
    AuthRoleListRequest roleListRequest = AuthRoleListRequest.getDefaultInstance();
    return FutureConverter.toCompletableFuture(this.stub.roleList(roleListRequest));
  }

  @Override
  public CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role,
      ByteSequence key, ByteSequence rangeEnd) {

    AuthRoleRevokePermissionRequest roleRevokePermissionRequest = AuthRoleRevokePermissionRequest
        .newBuilder()
        .setRoleBytes(byteStringFromByteSequence(role))
        .setKeyBytes(byteStringFromByteSequence(key))
        .setRangeEndBytes(byteStringFromByteSequence(rangeEnd))
        .build();
    return FutureConverter
        .toCompletableFuture(this.stub.roleRevokePermission(roleRevokePermissionRequest));
  }

  @Override
  public CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role) {

    AuthRoleDeleteRequest roleDeleteRequest = AuthRoleDeleteRequest.newBuilder()
        .setRoleBytes(byteStringFromByteSequence(role))
        .build();
    return FutureConverter.toCompletableFuture(this.stub.roleDelete(roleDeleteRequest));
  }

}
