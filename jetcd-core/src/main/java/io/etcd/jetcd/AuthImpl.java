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

import io.etcd.jetcd.api.AuthDisableRequest;
import io.etcd.jetcd.api.AuthEnableRequest;
import io.etcd.jetcd.api.AuthGrpc;
import io.etcd.jetcd.api.AuthRoleAddRequest;
import io.etcd.jetcd.api.AuthRoleDeleteRequest;
import io.etcd.jetcd.api.AuthRoleGetRequest;
import io.etcd.jetcd.api.AuthRoleGrantPermissionRequest;
import io.etcd.jetcd.api.AuthRoleListRequest;
import io.etcd.jetcd.api.AuthRoleRevokePermissionRequest;
import io.etcd.jetcd.api.AuthUserAddRequest;
import io.etcd.jetcd.api.AuthUserChangePasswordRequest;
import io.etcd.jetcd.api.AuthUserDeleteRequest;
import io.etcd.jetcd.api.AuthUserGetRequest;
import io.etcd.jetcd.api.AuthUserGrantRoleRequest;
import io.etcd.jetcd.api.AuthUserListRequest;
import io.etcd.jetcd.api.AuthUserRevokeRoleRequest;
import io.etcd.jetcd.api.Permission.Type;
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of etcd auth client.
 */
final class AuthImpl implements Auth {

    private final AuthGrpc.AuthFutureStub stub;
    private final ClientConnectionManager connectionManager;

    AuthImpl(ClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.stub = connectionManager.newStub(AuthGrpc::newFutureStub);
    }

    @Override
    public CompletableFuture<AuthEnableResponse> authEnable() {
        AuthEnableRequest enableRequest = AuthEnableRequest.getDefaultInstance();
        return Util.toCompletableFuture(this.stub.authEnable(enableRequest), AuthEnableResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthDisableResponse> authDisable() {
        AuthDisableRequest disableRequest = AuthDisableRequest.getDefaultInstance();
        return Util.toCompletableFuture(this.stub.authDisable(disableRequest), AuthDisableResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence user, ByteSequence password) {
        checkNotNull(user, "user can't be null");
        checkNotNull(password, "password can't be null");

        AuthUserAddRequest addRequest = AuthUserAddRequest.newBuilder().setNameBytes(user.getByteString())
            .setPasswordBytes(password.getByteString()).build();
        return Util.toCompletableFuture(this.stub.userAdd(addRequest), AuthUserAddResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence user) {
        checkNotNull(user, "user can't be null");

        AuthUserDeleteRequest deleteRequest = AuthUserDeleteRequest.newBuilder().setNameBytes(user.getByteString()).build();
        return Util.toCompletableFuture(this.stub.userDelete(deleteRequest), AuthUserDeleteResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence user, ByteSequence password) {
        checkNotNull(user, "user can't be null");
        checkNotNull(password, "password can't be null");

        AuthUserChangePasswordRequest changePasswordRequest = AuthUserChangePasswordRequest.newBuilder()
            .setNameBytes(user.getByteString()).setPasswordBytes(password.getByteString()).build();
        return Util.toCompletableFuture(this.stub.userChangePassword(changePasswordRequest),
            AuthUserChangePasswordResponse::new, this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthUserGetResponse> userGet(ByteSequence user) {
        checkNotNull(user, "user can't be null");

        AuthUserGetRequest userGetRequest = AuthUserGetRequest.newBuilder().setNameBytes(user.getByteString()).build();
        return Util.toCompletableFuture(this.stub.userGet(userGetRequest), AuthUserGetResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthUserListResponse> userList() {
        AuthUserListRequest userListRequest = AuthUserListRequest.getDefaultInstance();
        return Util.toCompletableFuture(this.stub.userList(userListRequest), AuthUserListResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence user, ByteSequence role) {
        checkNotNull(user, "user can't be null");
        checkNotNull(role, "key can't be null");

        AuthUserGrantRoleRequest userGrantRoleRequest = AuthUserGrantRoleRequest.newBuilder().setUserBytes(user.getByteString())
            .setRoleBytes(role.getByteString()).build();
        return Util.toCompletableFuture(this.stub.userGrantRole(userGrantRoleRequest), AuthUserGrantRoleResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence user, ByteSequence role) {
        checkNotNull(user, "user can't be null");
        checkNotNull(role, "key can't be null");

        AuthUserRevokeRoleRequest userRevokeRoleRequest = AuthUserRevokeRoleRequest.newBuilder()
            .setNameBytes(user.getByteString()).setRoleBytes(role.getByteString()).build();
        return Util.toCompletableFuture(this.stub.userRevokeRole(userRevokeRoleRequest), AuthUserRevokeRoleResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence user) {
        checkNotNull(user, "user can't be null");

        AuthRoleAddRequest roleAddRequest = AuthRoleAddRequest.newBuilder().setNameBytes(user.getByteString()).build();
        return Util.toCompletableFuture(this.stub.roleAdd(roleAddRequest), AuthRoleAddResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteSequence role, ByteSequence key,
        ByteSequence rangeEnd, Permission.Type permType) {
        checkNotNull(role, "role can't be null");
        checkNotNull(key, "key can't be null");
        checkNotNull(rangeEnd, "rangeEnd can't be null");
        checkNotNull(permType, "permType can't be null");

        io.etcd.jetcd.api.Permission.Type type;
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

        io.etcd.jetcd.api.Permission perm = io.etcd.jetcd.api.Permission.newBuilder().setKey(key.getByteString())
            .setRangeEnd(rangeEnd.getByteString()).setPermType(type).build();
        AuthRoleGrantPermissionRequest roleGrantPermissionRequest = AuthRoleGrantPermissionRequest.newBuilder()
            .setNameBytes(role.getByteString()).setPerm(perm).build();
        return Util.toCompletableFuture(this.stub.roleGrantPermission(roleGrantPermissionRequest),
            AuthRoleGrantPermissionResponse::new, this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role) {
        checkNotNull(role, "role can't be null");

        AuthRoleGetRequest roleGetRequest = AuthRoleGetRequest.newBuilder().setRoleBytes(role.getByteString()).build();
        return Util.toCompletableFuture(this.stub.roleGet(roleGetRequest), AuthRoleGetResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthRoleListResponse> roleList() {
        AuthRoleListRequest roleListRequest = AuthRoleListRequest.getDefaultInstance();
        return Util.toCompletableFuture(this.stub.roleList(roleListRequest), AuthRoleListResponse::new,
            this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role, ByteSequence key,
        ByteSequence rangeEnd) {
        checkNotNull(role, "role can't be null");
        checkNotNull(key, "key can't be null");
        checkNotNull(rangeEnd, "rangeEnd can't be null");

        AuthRoleRevokePermissionRequest roleRevokePermissionRequest = AuthRoleRevokePermissionRequest.newBuilder()
            .setRoleBytes(role.getByteString()).setKeyBytes(key.getByteString()).setRangeEndBytes(rangeEnd.getByteString())
            .build();
        return Util.toCompletableFuture(this.stub.roleRevokePermission(roleRevokePermissionRequest),
            AuthRoleRevokePermissionResponse::new, this.connectionManager.getExecutorService());
    }

    @Override
    public CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role) {
        checkNotNull(role, "role can't be null");
        AuthRoleDeleteRequest roleDeleteRequest = AuthRoleDeleteRequest.newBuilder().setRoleBytes(role.getByteString()).build();
        return Util.toCompletableFuture(this.stub.roleDelete(roleDeleteRequest), AuthRoleDeleteResponse::new,
            this.connectionManager.getExecutorService());
    }
}
