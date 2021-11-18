/*
 * Copyright 2016-2021 The jetcd authors
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
import io.etcd.jetcd.api.VertxAuthGrpc;
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
final class AuthImpl extends Impl implements Auth {

    private final VertxAuthGrpc.AuthVertxStub stub;

    AuthImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager.newStub(VertxAuthGrpc::newVertxStub);
    }

    @Override
    public CompletableFuture<AuthEnableResponse> authEnable() {
        AuthEnableRequest enableRequest = AuthEnableRequest.getDefaultInstance();
        return completable(
            this.stub.authEnable(enableRequest),
            AuthEnableResponse::new);
    }

    @Override
    public CompletableFuture<AuthDisableResponse> authDisable() {
        AuthDisableRequest disableRequest = AuthDisableRequest.getDefaultInstance();
        return completable(
            this.stub.authDisable(disableRequest),
            AuthDisableResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence user, ByteSequence password) {
        checkNotNull(user, "user can't be null");
        checkNotNull(password, "password can't be null");

        AuthUserAddRequest addRequest = AuthUserAddRequest.newBuilder()
            .setNameBytes(user.getByteString())
            .setPasswordBytes(password.getByteString())
            .build();

        return completable(
            this.stub.userAdd(addRequest),
            AuthUserAddResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence user) {
        checkNotNull(user, "user can't be null");

        AuthUserDeleteRequest deleteRequest = AuthUserDeleteRequest.newBuilder()
            .setNameBytes(user.getByteString())
            .build();

        return completable(
            this.stub.userDelete(deleteRequest),
            AuthUserDeleteResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence user, ByteSequence password) {
        checkNotNull(user, "user can't be null");
        checkNotNull(password, "password can't be null");

        AuthUserChangePasswordRequest changePasswordRequest = AuthUserChangePasswordRequest.newBuilder()
            .setNameBytes(user.getByteString())
            .setPasswordBytes(password.getByteString())
            .build();

        return completable(
            this.stub.userChangePassword(changePasswordRequest),
            AuthUserChangePasswordResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserGetResponse> userGet(ByteSequence user) {
        checkNotNull(user, "user can't be null");

        AuthUserGetRequest userGetRequest = AuthUserGetRequest.newBuilder()
            .setNameBytes(user.getByteString())
            .build();

        return completable(
            this.stub.userGet(userGetRequest),
            AuthUserGetResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserListResponse> userList() {
        AuthUserListRequest userListRequest = AuthUserListRequest.getDefaultInstance();

        return completable(
            this.stub.userList(userListRequest),
            AuthUserListResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence user, ByteSequence role) {
        checkNotNull(user, "user can't be null");
        checkNotNull(role, "key can't be null");

        AuthUserGrantRoleRequest userGrantRoleRequest = AuthUserGrantRoleRequest.newBuilder()
            .setUserBytes(user.getByteString())
            .setRoleBytes(role.getByteString())
            .build();

        return completable(
            this.stub.userGrantRole(userGrantRoleRequest),
            AuthUserGrantRoleResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence user, ByteSequence role) {
        checkNotNull(user, "user can't be null");
        checkNotNull(role, "key can't be null");

        AuthUserRevokeRoleRequest userRevokeRoleRequest = AuthUserRevokeRoleRequest.newBuilder()
            .setNameBytes(user.getByteString())
            .setRoleBytes(role.getByteString())
            .build();

        return completable(
            this.stub.userRevokeRole(userRevokeRoleRequest),
            AuthUserRevokeRoleResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence user) {
        checkNotNull(user, "user can't be null");

        AuthRoleAddRequest roleAddRequest = AuthRoleAddRequest.newBuilder().setNameBytes(user.getByteString()).build();

        return completable(
            this.stub.roleAdd(roleAddRequest),
            AuthRoleAddResponse::new);
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
                type = io.etcd.jetcd.api.Permission.Type.WRITE;
                break;
            case READWRITE:
                type = io.etcd.jetcd.api.Permission.Type.READWRITE;
                break;
            case READ:
                type = io.etcd.jetcd.api.Permission.Type.READ;
                break;
            default:
                type = io.etcd.jetcd.api.Permission.Type.UNRECOGNIZED;
                break;
        }

        io.etcd.jetcd.api.Permission perm = io.etcd.jetcd.api.Permission.newBuilder()
            .setKey(key.getByteString())
            .setRangeEnd(rangeEnd.getByteString())
            .setPermType(type)
            .build();

        AuthRoleGrantPermissionRequest roleGrantPermissionRequest = AuthRoleGrantPermissionRequest.newBuilder()
            .setNameBytes(role.getByteString())
            .setPerm(perm)
            .build();

        return completable(
            this.stub.roleGrantPermission(roleGrantPermissionRequest),
            AuthRoleGrantPermissionResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role) {
        checkNotNull(role, "role can't be null");

        AuthRoleGetRequest roleGetRequest = AuthRoleGetRequest.newBuilder()
            .setRoleBytes(role.getByteString())
            .build();

        return completable(
            this.stub.roleGet(roleGetRequest),
            AuthRoleGetResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleListResponse> roleList() {
        AuthRoleListRequest roleListRequest = AuthRoleListRequest.getDefaultInstance();

        return completable(
            this.stub.roleList(roleListRequest),
            AuthRoleListResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role, ByteSequence key,
        ByteSequence rangeEnd) {
        checkNotNull(role, "role can't be null");
        checkNotNull(key, "key can't be null");
        checkNotNull(rangeEnd, "rangeEnd can't be null");

        AuthRoleRevokePermissionRequest roleRevokePermissionRequest = AuthRoleRevokePermissionRequest.newBuilder()
            .setRoleBytes(role.getByteString())
            .setKeyBytes(key.getByteString())
            .setRangeEndBytes(rangeEnd.getByteString())
            .build();

        return completable(
            this.stub.roleRevokePermission(roleRevokePermissionRequest),
            AuthRoleRevokePermissionResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role) {
        checkNotNull(role, "role can't be null");
        AuthRoleDeleteRequest roleDeleteRequest = AuthRoleDeleteRequest.newBuilder()
            .setRoleBytes(role.getByteString())
            .build();

        return completable(
            this.stub.roleDelete(roleDeleteRequest),
            AuthRoleDeleteResponse::new);
    }
}
