/*
 * Copyright 2016-2020 The jetcd authors
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

import java.util.concurrent.CompletableFuture;

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
import io.etcd.jetcd.support.CloseableClient;

/**
 * Interface of auth talking to etcd.
 */
public interface Auth extends CloseableClient {

    /**
     * enables auth of an etcd cluster.
     *
     * @return the response.
     */
    CompletableFuture<AuthEnableResponse> authEnable();

    /**
     * disables auth of an etcd cluster.
     *
     * @return the response.
     */
    CompletableFuture<AuthDisableResponse> authDisable();

    /**
     * adds a new user to an etcd cluster.
     *
     * @param  user     the user
     * @param  password the password
     * @return          the response.
     */
    CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence user, ByteSequence password);

    /**
     * deletes a user from an etcd cluster.
     *
     * @param  user the user
     * @return      the response.
     */
    CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence user);

    /**
     * changes a password of a user.
     *
     * @param  user     the user
     * @param  password the password
     * @return          the response.
     */
    CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence user, ByteSequence password);

    /**
     * gets a detailed information of a user.
     *
     * @param  user the user
     * @return      the response.
     */
    CompletableFuture<AuthUserGetResponse> userGet(ByteSequence user);

    /**
     * gets a list of all users.
     *
     * @return the response.
     */
    CompletableFuture<AuthUserListResponse> userList();

    /**
     * grants a role to a user.
     *
     * @param  user the user
     * @param  role the role to grant
     * @return      the response.
     */
    CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence user, ByteSequence role);

    /**
     * revokes a role of a user.
     *
     * @param  user the user
     * @param  role the role to revoke
     * @return      the response.
     */
    CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence user, ByteSequence role);

    /**
     * adds a new role to an etcd cluster.
     *
     * @param  role the role to add
     * @return      the response.
     */
    CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence role);

    /**
     * grants a permission to a role.
     *
     * @param  role     the role
     * @param  key      the key
     * @param  rangeEnd the range end
     * @param  permType the type
     * @return          the response.
     */
    CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteSequence role, ByteSequence key,
        ByteSequence rangeEnd, Permission.Type permType);

    /**
     * gets a detailed information of a role.
     *
     * @param  role the role to get
     * @return      the response.
     */
    CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role);

    /**
     * gets a list of all roles.
     *
     * @return the response.
     */
    CompletableFuture<AuthRoleListResponse> roleList();

    /**
     * revokes a permission from a role.
     *
     * @param  role     the role
     * @param  key      the key
     * @param  rangeEnd the range end
     * @return          the response.
     */
    CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role, ByteSequence key,
        ByteSequence rangeEnd);

    /**
     * RoleDelete deletes a role.
     *
     * @param  role the role to delete.
     * @return      the response.
     */
    CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role);

}
