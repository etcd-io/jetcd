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

package io.etcd.jetcd.impl;

import java.util.concurrent.CompletableFuture;

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.ByteSequence;
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
import io.etcd.jetcd.grpc.GrpcService;

import com.google.protobuf.ByteString;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of etcd auth client.
 */
final class AuthClient extends AbstractClient implements Auth {

    private final io.etcd.jetcd.api.AuthGrpcClient client;

    AuthClient(GrpcService grpcService) {
        super(grpcService);

        this.client = io.etcd.jetcd.api.AuthGrpcClient.create(
            grpcService.getAuthenticatedGrpcClient(),
            grpcService.getServiceResolver().getTarget());
    }

    @Override
    public CompletableFuture<AuthEnableResponse> authEnable() {
        io.etcd.jetcd.api.AuthEnableRequest enableRequest = io.etcd.jetcd.api.AuthEnableRequest.getDefaultInstance();
        return completable(
            client.authEnable(enableRequest),
            AuthEnableResponse::new);
    }

    @Override
    public CompletableFuture<AuthDisableResponse> authDisable() {
        io.etcd.jetcd.api.AuthDisableRequest disableRequest = io.etcd.jetcd.api.AuthDisableRequest.getDefaultInstance();
        return completable(
            client.authDisable(disableRequest),
            AuthDisableResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserAddResponse> userAdd(ByteSequence user, ByteSequence password) {
        requireNonNull(user, "user can't be null");
        requireNonNull(password, "password can't be null");

        io.etcd.jetcd.api.AuthUserAddRequest addRequest = io.etcd.jetcd.api.AuthUserAddRequest.newBuilder()
            .setNameBytes(ByteString.copyFrom(user.getBytes()))
            .setPasswordBytes(ByteString.copyFrom(password.getBytes()))
            .build();

        return completable(
            client.userAdd(addRequest),
            AuthUserAddResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserDeleteResponse> userDelete(ByteSequence user) {
        requireNonNull(user, "user can't be null");

        io.etcd.jetcd.api.AuthUserDeleteRequest deleteRequest = io.etcd.jetcd.api.AuthUserDeleteRequest.newBuilder()
            .setNameBytes(ByteString.copyFrom(user.getBytes()))
            .build();

        return completable(
            client.userDelete(deleteRequest),
            AuthUserDeleteResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteSequence user, ByteSequence password) {
        requireNonNull(user, "user can't be null");
        requireNonNull(password, "password can't be null");

        io.etcd.jetcd.api.AuthUserChangePasswordRequest changePasswordRequest = io.etcd.jetcd.api.AuthUserChangePasswordRequest
            .newBuilder()
            .setNameBytes(ByteString.copyFrom(user.getBytes()))
            .setPasswordBytes(ByteString.copyFrom(password.getBytes()))
            .build();

        return completable(
            client.userChangePassword(changePasswordRequest),
            AuthUserChangePasswordResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserGetResponse> userGet(ByteSequence user) {
        requireNonNull(user, "user can't be null");

        io.etcd.jetcd.api.AuthUserGetRequest userGetRequest = io.etcd.jetcd.api.AuthUserGetRequest.newBuilder()
            .setNameBytes(ByteString.copyFrom(user.getBytes()))
            .build();

        return completable(
            client.userGet(userGetRequest),
            AuthUserGetResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserListResponse> userList() {
        io.etcd.jetcd.api.AuthUserListRequest userListRequest = io.etcd.jetcd.api.AuthUserListRequest.getDefaultInstance();

        return completable(
            client.userList(userListRequest),
            AuthUserListResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteSequence user, ByteSequence role) {
        requireNonNull(user, "user can't be null");
        requireNonNull(role, "key can't be null");

        io.etcd.jetcd.api.AuthUserGrantRoleRequest userGrantRoleRequest = io.etcd.jetcd.api.AuthUserGrantRoleRequest
            .newBuilder()
            .setUserBytes(ByteString.copyFrom(user.getBytes()))
            .setRoleBytes(ByteString.copyFrom(role.getBytes()))
            .build();

        return completable(
            client.userGrantRole(userGrantRoleRequest),
            AuthUserGrantRoleResponse::new);
    }

    @Override
    public CompletableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteSequence user, ByteSequence role) {
        requireNonNull(user, "user can't be null");
        requireNonNull(role, "key can't be null");

        io.etcd.jetcd.api.AuthUserRevokeRoleRequest userRevokeRoleRequest = io.etcd.jetcd.api.AuthUserRevokeRoleRequest
            .newBuilder()
            .setNameBytes(ByteString.copyFrom(user.getBytes()))
            .setRoleBytes(ByteString.copyFrom(role.getBytes()))
            .build();

        return completable(
            client.userRevokeRole(userRevokeRoleRequest),
            AuthUserRevokeRoleResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleAddResponse> roleAdd(ByteSequence user) {
        requireNonNull(user, "user can't be null");

        io.etcd.jetcd.api.AuthRoleAddRequest roleAddRequest = io.etcd.jetcd.api.AuthRoleAddRequest.newBuilder()
            .setNameBytes(ByteString.copyFrom(user.getBytes()))
            .build();

        return completable(
            client.roleAdd(roleAddRequest),
            AuthRoleAddResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteSequence role, ByteSequence key,
        ByteSequence rangeEnd, Permission.Type permType) {
        requireNonNull(role, "role can't be null");
        requireNonNull(key, "key can't be null");
        requireNonNull(rangeEnd, "rangeEnd can't be null");
        requireNonNull(permType, "permType can't be null");

        io.etcd.jetcd.api.Permission.Type type = switch (permType) {
            case WRITE -> io.etcd.jetcd.api.Permission.Type.WRITE;
            case READWRITE -> io.etcd.jetcd.api.Permission.Type.READWRITE;
            case READ -> io.etcd.jetcd.api.Permission.Type.READ;
            default -> io.etcd.jetcd.api.Permission.Type.UNRECOGNIZED;
        };

        io.etcd.jetcd.api.Permission perm = io.etcd.jetcd.api.Permission.newBuilder()
            .setKey(ByteString.copyFrom(key.getBytes()))
            .setRangeEnd(ByteString.copyFrom(rangeEnd.getBytes()))
            .setPermType(type)
            .build();

        io.etcd.jetcd.api.AuthRoleGrantPermissionRequest roleGrantPermissionRequest = io.etcd.jetcd.api.AuthRoleGrantPermissionRequest
            .newBuilder()
            .setNameBytes(ByteString.copyFrom(role.getBytes()))
            .setPerm(perm)
            .build();

        return completable(
            client.roleGrantPermission(roleGrantPermissionRequest),
            AuthRoleGrantPermissionResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleGetResponse> roleGet(ByteSequence role) {
        requireNonNull(role, "role can't be null");

        io.etcd.jetcd.api.AuthRoleGetRequest roleGetRequest = io.etcd.jetcd.api.AuthRoleGetRequest.newBuilder()
            .setRoleBytes(ByteString.copyFrom(role.getBytes()))
            .build();

        return completable(
            client.roleGet(roleGetRequest),
            AuthRoleGetResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleListResponse> roleList() {
        io.etcd.jetcd.api.AuthRoleListRequest roleListRequest = io.etcd.jetcd.api.AuthRoleListRequest.getDefaultInstance();

        return completable(
            client.roleList(roleListRequest),
            AuthRoleListResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteSequence role, ByteSequence key,
        ByteSequence rangeEnd) {
        requireNonNull(role, "role can't be null");
        requireNonNull(key, "key can't be null");
        requireNonNull(rangeEnd, "rangeEnd can't be null");

        io.etcd.jetcd.api.AuthRoleRevokePermissionRequest roleRevokePermissionRequest = io.etcd.jetcd.api.AuthRoleRevokePermissionRequest
            .newBuilder()
            .setRoleBytes(ByteString.copyFrom(role.getBytes()))
            .setKey(ByteString.copyFrom(key.getBytes()))
            .setRangeEnd(ByteString.copyFrom(rangeEnd.getBytes()))
            .build();

        return completable(
            client.roleRevokePermission(roleRevokePermissionRequest),
            AuthRoleRevokePermissionResponse::new);
    }

    @Override
    public CompletableFuture<AuthRoleDeleteResponse> roleDelete(ByteSequence role) {
        requireNonNull(role, "role can't be null");
        io.etcd.jetcd.api.AuthRoleDeleteRequest roleDeleteRequest = io.etcd.jetcd.api.AuthRoleDeleteRequest.newBuilder()
            .setRoleBytes(ByteString.copyFrom(role.getBytes()))
            .build();

        return completable(
            client.roleDelete(roleDeleteRequest),
            AuthRoleDeleteResponse::new);
    }
}
