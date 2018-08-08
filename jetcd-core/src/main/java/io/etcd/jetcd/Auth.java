/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd;

import io.etcd.jetcd.auth.AuthDisableResponse;
import io.etcd.jetcd.auth.AuthEnableResponse;
import io.etcd.jetcd.auth.AuthRoleAddResponse;
import io.etcd.jetcd.auth.AuthRoleDeleteResponse;
import io.etcd.jetcd.auth.AuthRoleGetResponse;
import io.etcd.jetcd.auth.AuthRoleGrantPermissionResponse;
import io.etcd.jetcd.auth.AuthRoleListResponse;
import io.etcd.jetcd.auth.AuthRoleRevokePermissionResponse;
import io.etcd.jetcd.auth.AuthUserAddResponse;
import io.etcd.jetcd.auth.AuthUserChangePasswordResponse;
import io.etcd.jetcd.auth.AuthUserDeleteResponse;
import io.etcd.jetcd.auth.AuthUserGetResponse;
import io.etcd.jetcd.auth.AuthUserGrantRoleResponse;
import io.etcd.jetcd.auth.AuthUserListResponse;
import io.etcd.jetcd.auth.AuthUserRevokeRoleResponse;
import io.etcd.jetcd.auth.Permission;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.internal.impl.CloseableClient;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of auth talking to etcd.
 */
public interface Auth extends CloseableClient {

  /**
   * enables auth of an etcd cluster.
   */
  CompletableFuture<AuthEnableResponse> authEnable();

  /**
   * disables auth of an etcd cluster.
   */
  CompletableFuture<AuthDisableResponse> authDisable();

  /**
   * adds a new user to an etcd cluster.
   */
  CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence user, ByteSequence password);

  /**
   * deletes a user from an etcd cluster.
   */
  CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence user);

  /**
   * changes a password of a user.
   */
  CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence user,
      ByteSequence password);

  /**
   * gets a detailed information of a user.
   */
  CompletableFuture<AuthUserGetResponse> userGet(ByteSequence user);

  /**
   * gets a list of all users.
   */
  CompletableFuture<AuthUserListResponse> userList();

  /**
   * grants a role to a user.
   */
  CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence user, ByteSequence role);

  /**
   * revokes a role of a user.
   */
  CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence user,
      ByteSequence role);

  /**
   * adds a new role to an etcd cluster.
   */
  CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence user);

  /**
   * grants a permission to a role.
   */
  CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteSequence role,
      ByteSequence key,
      ByteSequence rangeEnd, Permission.Type permType);

  /**
   * gets a detailed information of a role.
   */
  CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role);

  /**
   * gets a list of all roles.
   */
  CompletableFuture<AuthRoleListResponse> roleList();

  /**
   * revokes a permission from a role.
   */
  CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role,
      ByteSequence key,
      ByteSequence rangeEnd);

  /**
   * RoleDelete deletes a role.
   */
  CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role);

}
