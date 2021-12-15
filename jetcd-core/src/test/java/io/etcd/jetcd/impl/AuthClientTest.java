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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.auth.AuthRoleGetResponse;
import io.etcd.jetcd.auth.AuthRoleListResponse;
import io.etcd.jetcd.auth.Permission;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class AuthClientTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    private static final String rootString = "root";
    private static final ByteSequence rootPass = bytesOf("123");
    private static final String rootRoleString = "root";
    private static final String userString = "user";
    private static final String userRoleString = "userRole";
    private static Auth authDisabledAuthClient;
    private static KV authDisabledKVClient;
    private final ByteSequence rootRoleKey = bytesOf("root");
    private final ByteSequence rootRoleValue = bytesOf("b");
    private final ByteSequence rootRoleKeyRangeBegin = bytesOf("root");
    private final ByteSequence rootRoleKeyRangeEnd = bytesOf("root1");
    private final ByteSequence userRoleKey = bytesOf("foo");
    private final ByteSequence userRoleValue = bytesOf("bar");
    private final ByteSequence userRoleKeyRangeBegin = bytesOf("foo");
    private final ByteSequence userRoleKeyRangeEnd = bytesOf("foo1");
    private final ByteSequence root = bytesOf(rootString);
    private final ByteSequence rootRole = bytesOf(rootRoleString);
    private final ByteSequence user = bytesOf(userString);
    private final ByteSequence userPass = bytesOf("userPass");
    private final ByteSequence userNewPass = bytesOf("newUserPass");
    private final ByteSequence userRole = bytesOf(userRoleString);

    /**
     * Build etcd client to create role, permission.
     */
    @BeforeAll
    public static void setupEnv() {
        Client client = TestUtil.client(cluster).build();
        authDisabledKVClient = client.getKVClient();
        authDisabledAuthClient = client.getAuthClient();
    }

    @Test
    public void testAuth() throws Exception {
        authDisabledAuthClient.roleAdd(rootRole).get();
        authDisabledAuthClient.roleAdd(userRole).get();

        final AuthRoleListResponse response = authDisabledAuthClient.roleList().get();
        assertThat(response.getRoles()).containsOnly(rootRoleString, userRoleString);

        authDisabledAuthClient
            .roleGrantPermission(rootRole, rootRoleKeyRangeBegin, rootRoleKeyRangeEnd, Permission.Type.READWRITE).get();
        authDisabledAuthClient
            .roleGrantPermission(userRole, userRoleKeyRangeBegin, userRoleKeyRangeEnd, Permission.Type.READWRITE).get();

        authDisabledAuthClient.userAdd(root, rootPass).get();
        authDisabledAuthClient.userAdd(user, userPass).get();

        authDisabledAuthClient.userChangePassword(user, userNewPass).get();

        List<String> users = authDisabledAuthClient.userList().get().getUsers();
        assertThat(users).containsOnly(rootString, userString);

        authDisabledAuthClient.userGrantRole(root, rootRole).get();
        authDisabledAuthClient.userGrantRole(user, rootRole).get();
        authDisabledAuthClient.userGrantRole(user, userRole).get();

        assertThat(authDisabledAuthClient.userGet(root).get().getRoles()).containsOnly(rootRoleString);
        assertThat(authDisabledAuthClient.userGet(user).get().getRoles()).containsOnly(rootRoleString, userRoleString);

        authDisabledAuthClient.authEnable().get();

        final Client userClient = TestUtil.client(cluster).user(user).password(userNewPass).build();
        final Client rootClient = TestUtil.client(cluster).user(root).password(rootPass).build();

        userClient.getKVClient().put(rootRoleKey, rootRoleValue).get();
        userClient.getKVClient().put(userRoleKey, userRoleValue).get();
        userClient.getKVClient().get(rootRoleKey).get();
        userClient.getKVClient().get(userRoleKey).get();

        assertThatThrownBy(() -> authDisabledKVClient.put(rootRoleKey, rootRoleValue).get())
            .hasMessageContaining("etcdserver: user name is empty");
        assertThatThrownBy(() -> authDisabledKVClient.put(userRoleKey, rootRoleValue).get())
            .hasMessageContaining("etcdserver: user name is empty");
        assertThatThrownBy(() -> authDisabledKVClient.get(rootRoleKey).get())
            .hasMessageContaining("etcdserver: user name is empty");
        assertThatThrownBy(() -> authDisabledKVClient.get(userRoleKey).get())
            .hasMessageContaining("etcdserver: user name is empty");

        AuthRoleGetResponse roleGetResponse = userClient.getAuthClient().roleGet(rootRole).get();
        assertThat(roleGetResponse.getPermissions().size()).isNotEqualTo(0);

        roleGetResponse = userClient.getAuthClient().roleGet(userRole).get();
        assertThat(roleGetResponse.getPermissions().size()).isNotEqualTo(0);

        rootClient.getAuthClient().userRevokeRole(user, rootRole).get();

        final KV kvClient = userClient.getKVClient();
        // verify the access to root role is revoked for user.
        assertThatThrownBy(() -> kvClient.get(rootRoleKey).get()).isNotNull();
        // verify userRole is still valid.
        assertThat(kvClient.get(userRoleKey).get().getCount()).isNotEqualTo(0);

        rootClient.getAuthClient().roleRevokePermission(userRole, userRoleKeyRangeBegin, userRoleKeyRangeEnd).get();

        // verify the access to foo is revoked for user.
        assertThatThrownBy(() -> userClient.getKVClient().get(userRoleKey).get()).isNotNull();

        rootClient.getAuthClient().authDisable().get();

        authDisabledAuthClient.userDelete(root).get();
        authDisabledAuthClient.userDelete(user).get();

        authDisabledAuthClient.roleDelete(rootRole).get();
        authDisabledAuthClient.roleDelete(userRole).get();
    }
}
