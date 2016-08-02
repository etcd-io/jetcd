package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

/**
 * Interface of auth talking to etcd
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

    ListenableFuture<AuthUserChangePasswordResponse> userChangePassword(ByteString name, ByteString password);

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

    ListenableFuture<AuthRoleGrantPermissionResponse> roleGrantPermission(ByteString role, ByteString key,
                                                                          ByteString rangeEnd, Permission.Type permType);

    ListenableFuture<AuthRoleGetResponse> roleGet(ByteString role);

    ListenableFuture<AuthRoleListResponse> roleList();

    ListenableFuture<AuthRoleRevokePermissionResponse> roleRevokePermission(ByteString role, ByteString key,
                                                                            ByteString rangeEnd);

    ListenableFuture<AuthRoleDeleteResponse> roleDelete(ByteString role);

}
