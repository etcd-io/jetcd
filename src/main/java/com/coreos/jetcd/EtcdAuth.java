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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

/**
 * Interface of auth talking to etcd.
 */
public interface EtcdAuth {

  // ***************
  // Auth Manage
  // ***************

  ListenableFuture<AuthEnableResponse> authEnable();

  ListenableFuture<AuthDisableResponse> authDisable();

  // ***************
  // User Manage
  // ***************

  ListenableFuture<AuthUserAddResponse> userAdd(ByteString name, ByteString password);

  ListenableFuture<AuthUserDeleteResponse> userDelete(ByteString name);

  ListenableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteString name,
      ByteString password);

  ListenableFuture<AuthUserGetResponse> userGet(ByteString name);

  ListenableFuture<AuthUserListResponse> userList();

  // ***************
  // User Role Manage
  // ***************

  ListenableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteString name, ByteString role);

  ListenableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteString name, ByteString role);

  // ***************
  // Role Manage
  // ***************

  ListenableFuture<AuthRoleAddResponse> roleAdd(ByteString name);

  ListenableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteString role,
      ByteString key,
      ByteString rangeEnd, Permission.Type permType);

  ListenableFuture<AuthRoleGetResponse> roleGet(ByteString role);

  ListenableFuture<AuthRoleListResponse> roleList();

  ListenableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteString role,
      ByteString key,
      ByteString rangeEnd);

  ListenableFuture<AuthRoleDeleteResponse> roleDelete(ByteString role);

}
