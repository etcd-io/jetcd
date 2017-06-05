package com.coreos.jetcd;

import com.coreos.jetcd.api.AuthDisableResponse;
import com.coreos.jetcd.api.AuthEnableResponse;
import com.coreos.jetcd.api.AuthRoleAddResponse;
import com.coreos.jetcd.api.AuthRoleDeleteResponse;
import com.coreos.jetcd.api.AuthRoleGetResponse;
import com.coreos.jetcd.api.AuthRoleGrantPermissionResponse;
import com.coreos.jetcd.api.AuthRoleListResponse;
import com.coreos.jetcd.api.AuthRoleRevokePermissionResponse;
import com.coreos.jetcd.api.AuthUserAddResponse;
import com.coreos.jetcd.api.AuthUserChangePasswordResponse;
import com.coreos.jetcd.api.AuthUserDeleteResponse;
import com.coreos.jetcd.api.AuthUserGetResponse;
import com.coreos.jetcd.api.AuthUserGrantRoleResponse;
import com.coreos.jetcd.api.AuthUserListResponse;
import com.coreos.jetcd.api.AuthUserRevokeRoleResponse;
import com.coreos.jetcd.api.Permission;
import com.google.protobuf.ByteString;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of auth talking to etcd.
 */
public interface Auth {

  // ***************
  // Auth Manage
  // ***************

  CompletableFuture<AuthEnableResponse> authEnable();

  CompletableFuture<AuthDisableResponse> authDisable();

  // ***************
  // User Manage
  // ***************

  CompletableFuture<AuthUserAddResponse> userAdd(ByteString name, ByteString password);

  CompletableFuture<AuthUserDeleteResponse> userDelete(ByteString name);

  CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteString name,
      ByteString password);

  CompletableFuture<AuthUserGetResponse> userGet(ByteString name);

  CompletableFuture<AuthUserListResponse> userList();

  // ***************
  // User Role Manage
  // ***************

  CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteString name, ByteString role);

  CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteString name, ByteString role);

  // ***************
  // Role Manage
  // ***************

  CompletableFuture<AuthRoleAddResponse> roleAdd(ByteString name);

  CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteString role,
      ByteString key,
      ByteString rangeEnd, Permission.Type permType);

  CompletableFuture<AuthRoleGetResponse> roleGet(ByteString role);

  CompletableFuture<AuthRoleListResponse> roleList();

  CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteString role,
      ByteString key,
      ByteString rangeEnd);

  CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteString role);

}
