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
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.internal.impl.CloseableClient;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of auth talking to etcd.
 */
public interface Auth extends CloseableClient {

  // ***************
  // Auth Manage
  // ***************

  CompletableFuture<AuthEnableResponse> authEnable();

  CompletableFuture<AuthDisableResponse> authDisable();

  // ***************
  // User Manage
  // ***************

  CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence name, ByteSequence password);

  CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence name);

  CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence name,
      ByteSequence password);

  CompletableFuture<AuthUserGetResponse> userGet(ByteSequence name);

  CompletableFuture<AuthUserListResponse> userList();

  // ***************
  // User Role Manage
  // ***************

  CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence name, ByteSequence role);

  CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence name,
      ByteSequence role);

  // ***************
  // Role Manage
  // ***************

  CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence name);

  CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteSequence role,
      ByteSequence key,
      ByteSequence rangeEnd, Permission.Type permType);

  CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role);

  CompletableFuture<AuthRoleListResponse> roleList();

  CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role,
      ByteSequence key,
      ByteSequence rangeEnd);

  CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role);

}
