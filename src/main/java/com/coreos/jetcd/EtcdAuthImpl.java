package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;

/**
 * Implementation of etcd auth client
 */
public class EtcdAuthImpl implements EtcdAuth {

    private AuthGrpc.AuthFutureStub authStub;


    public EtcdAuthImpl(AuthGrpc.AuthFutureStub authStub) {
        this.authStub = authStub;
    }

    // ***************
    // Auth Manage
    // ***************

    @Override
    public ListenableFuture<AuthEnableResponse> authEnable() {
        AuthEnableRequest enableRequest = AuthEnableRequest.getDefaultInstance();
        return authStub.authEnable(enableRequest);
    }

    @Override
    public ListenableFuture<AuthDisableResponse> authDisable() {
        AuthDisableRequest disableRequest = AuthDisableRequest.getDefaultInstance();
        return authStub.authDisable(disableRequest);
    }

    // ***************
    // User Manage
    // ***************

    @Override
    public ListenableFuture<AuthUserAddResponse> userAdd(ByteString name, ByteString password) {
        AuthUserAddRequest addRequest = AuthUserAddRequest.newBuilder()
                .setNameBytes(name)
                .setPasswordBytes(password)
                .build();
        return authStub.userAdd(addRequest);
    }

    @Override
    public ListenableFuture<AuthUserDeleteResponse> userDelete(ByteString name) {
        AuthUserDeleteRequest deleteRequest = AuthUserDeleteRequest.newBuilder()
                .setNameBytes(name).build();
        return authStub.userDelete(deleteRequest);
    }

    @Override
    public ListenableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteString name, ByteString password) {
        AuthUserChangePasswordRequest changePasswordRequest = AuthUserChangePasswordRequest.newBuilder()
                .setNameBytes(name)
                .setPasswordBytes(password)
                .build();
        return authStub.userChangePassword(changePasswordRequest);
    }

    @Override
    public ListenableFuture<AuthUserGetResponse> userGet(ByteString name) {
        AuthUserGetRequest userGetRequest = AuthUserGetRequest.newBuilder()
                .setNameBytes(name)
                .build();

        return authStub.userGet(userGetRequest);
    }

    @Override
    public ListenableFuture<AuthUserListResponse> userList() {
        AuthUserListRequest userListRequest = AuthUserListRequest.getDefaultInstance();
        return authStub.userList(userListRequest);
    }

    // ***************
    // User Role Manage
    // ***************

    @Override
    public ListenableFuture<AuthUserGrantRoleResponse> userGrantRole(ByteString name, ByteString role) {
        AuthUserGrantRoleRequest userGrantRoleRequest = AuthUserGrantRoleRequest.newBuilder()
                .setUserBytes(name)
                .setRoleBytes(role)
                .build();
        return authStub.userGrantRole(userGrantRoleRequest);
    }

    @Override
    public ListenableFuture<AuthUserRevokeRoleResponse> userRevokeRole(ByteString name, ByteString role) {
        AuthUserRevokeRoleRequest userRevokeRoleRequest = AuthUserRevokeRoleRequest.newBuilder()
                .setNameBytes(name)
                .setRoleBytes(role)
                .build();
        return authStub.userRevokeRole(userRevokeRoleRequest);
    }

    // ***************
    // Role Manage
    // ***************

    @Override
    public ListenableFuture<AuthRoleAddResponse> roleAdd(ByteString name) {
        AuthRoleAddRequest roleAddRequest = AuthRoleAddRequest.newBuilder()
                .setNameBytes(name)
                .build();
        return authStub.roleAdd(roleAddRequest);
    }

    @Override
    public ListenableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteString role, ByteString key, ByteString rangeEnd, Permission.Type permType) {
        Permission perm = Permission.newBuilder()
                .setKey(key)
                .setRangeEnd(rangeEnd)
                .setPermType(permType)
                .build();
        AuthRoleGrantPermissionRequest roleGrantPermissionRequest = AuthRoleGrantPermissionRequest.newBuilder()
                .setNameBytes(role)
                .setPerm(perm)
                .build();

        return authStub.roleGrantPermission(roleGrantPermissionRequest);
    }

    @Override
    public ListenableFuture<AuthRoleGetResponse> roleGet(ByteString role) {
        AuthRoleGetRequest roleGetRequest = AuthRoleGetRequest.newBuilder()
                .setRoleBytes(role)
                .build();
        return authStub.roleGet(roleGetRequest);
    }

    @Override
    public ListenableFuture<AuthRoleListResponse> roleList() {
        AuthRoleListRequest roleListRequest = AuthRoleListRequest.getDefaultInstance();
        return authStub.roleList(roleListRequest);
    }

    @Override
    public ListenableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteString role, ByteString key, ByteString rangeEnd) {

        AuthRoleRevokePermissionRequest roleRevokePermissionRequest = AuthRoleRevokePermissionRequest.newBuilder()
                .setRoleBytes(role)
                .setKeyBytes(key)
                .setRangeEndBytes(rangeEnd)
                .build();
        return authStub.roleRevokePermission(roleRevokePermissionRequest);
    }

    @Override
    public ListenableFuture<AuthRoleDeleteResponse> roleDelete(ByteString role) {

        AuthRoleDeleteRequest roleDeleteRequest = AuthRoleDeleteRequest.newBuilder()
                .setRoleBytes(role)
                .build();
        return authStub.roleDelete(roleDeleteRequest);
    }

}
