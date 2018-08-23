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
package io.etcd.jetcd.internal.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.base.Charsets;
import io.etcd.jetcd.Auth;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.auth.AuthRoleGetResponse;
import io.etcd.jetcd.auth.AuthRoleListResponse;
import io.etcd.jetcd.auth.Permission;
import io.etcd.jetcd.auth.Permission.Type;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.internal.infrastructure.EtcdCluster;
import io.etcd.jetcd.internal.infrastructure.EtcdClusterFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * test etcd auth
 */
public class AuthClientTest {

  private static final EtcdCluster CLUSTER = EtcdClusterFactory.buildCluster("auth-etcd", 1, false);

  private final ByteSequence rootRolekeyRangeBegin = ByteSequence.from("root", Charsets.UTF_8);
  private final ByteSequence rootkeyRangeEnd = ByteSequence.from("root1", Charsets.UTF_8);

  private final ByteSequence userRolekeyRangeBegin = ByteSequence.from("foo", Charsets.UTF_8);
  private final ByteSequence userRolekeyRangeEnd = ByteSequence.from("foo1", Charsets.UTF_8);

  private final ByteSequence rootRoleKey = ByteSequence.from("root", Charsets.UTF_8);
  private final ByteSequence rootRoleValue = ByteSequence.from("b", Charsets.UTF_8);

  private final ByteSequence userRoleKey = ByteSequence.from("foo", Charsets.UTF_8);
  private final ByteSequence userRoleValue = ByteSequence.from("bar", Charsets.UTF_8);


  private final ByteSequence root = ByteSequence.from("root", Charsets.UTF_8);
  private final ByteSequence rootPass = ByteSequence.from("123", Charsets.UTF_8);
  private final ByteSequence rootRole = ByteSequence.from("root", Charsets.UTF_8);


  private final ByteSequence user = ByteSequence.from("user", Charsets.UTF_8);
  private final ByteSequence userPass = ByteSequence.from("userPass", Charsets.UTF_8);
  private final ByteSequence userNewPass = ByteSequence.from("newUserPass", Charsets.UTF_8);
  private final ByteSequence userRole = ByteSequence.from("userRole", Charsets.UTF_8);

  private Client userClient;
  private Client rootClient;

  private Auth authDisabledAuthClient;
  private KV authDisabledKVClient;

  private List<String> endpoints;

  /**
   * Build etcd client to create role, permission
   */
  @BeforeTest
  public void setupEnv() {
    CLUSTER.start();
    endpoints = CLUSTER.getClientEndpoints();
    Client client = Client.builder()
        .endpoints(endpoints)
        .build();

    this.authDisabledKVClient = client.getKVClient();
    this.authDisabledAuthClient = client.getAuthClient();
  }

  /**
   * create role with un-auth etcd client
   */
  @Test(groups = "role", priority = 1)
  public void testRoleAdd() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient.roleAdd(rootRole).get();
    this.authDisabledAuthClient.roleAdd(userRole).get();
  }

  @Test(dependsOnMethods = "testRoleAdd", groups = "role", priority = 1)
  public void testRoleList() throws ExecutionException, InterruptedException {
    AuthRoleListResponse response = this.authDisabledAuthClient.roleList().get();
    assertThat(response.getRoles().get(0)).isEqualTo(this.rootRole.toStringUtf8());
  }


  /**
   * grant permission to role
   */
  @Test(dependsOnMethods = "testRoleAdd", groups = "role", priority = 1)
  public void testRoleGrantPermission() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient
        .roleGrantPermission(rootRole, rootRolekeyRangeBegin, rootkeyRangeEnd,
            Permission.Type.READWRITE).get();
    this.authDisabledAuthClient
        .roleGrantPermission(userRole, userRolekeyRangeBegin, userRolekeyRangeEnd, Type.READWRITE)
        .get();
  }

  /**
   * add user with rootPass and username
   */
  @Test(groups = "user", priority = 1)
  public void testUserAdd() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient.userAdd(root, rootPass).get();
    this.authDisabledAuthClient.userAdd(user, userPass).get();
  }

  @Test(dependsOnMethods = "testUserAdd", groups = "user", priority = 1)
  public void testUserChangePassword() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient.userChangePassword(user, userNewPass).get();
  }


  @Test(dependsOnMethods = "testUserAdd", groups = "user", priority = 1)
  public void testUserList() throws ExecutionException, InterruptedException {
    List<String> users = this.authDisabledAuthClient.userList().get().getUsers();
    assertThat(users.get(0)).isEqualTo(this.root.toStringUtf8());
    assertThat(users.get(1)).isEqualTo(this.user.toStringUtf8());
  }

  /**
   * grant user role
   */
  @Test(dependsOnMethods = {"testUserAdd",
      "testRoleGrantPermission"}, groups = "user", priority = 1)
  public void testUserGrantRole() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient.userGrantRole(root, rootRole).get();
    this.authDisabledAuthClient.userGrantRole(user, rootRole).get();
    this.authDisabledAuthClient.userGrantRole(user, userRole).get();
  }

  @Test(dependsOnMethods = "testUserGrantRole", groups = "user", priority = 1)
  public void testUserGet() throws ExecutionException, InterruptedException {
    assertThat(this.authDisabledAuthClient.userGet(root).get().getRoles().get(0))
        .isEqualTo(rootRole.toStringUtf8());
    assertThat(this.authDisabledAuthClient.userGet(user).get().getRoles().get(0))
        .isEqualTo(rootRole.toStringUtf8());
    assertThat(this.authDisabledAuthClient.userGet(user).get().getRoles().get(1))
        .isEqualTo(userRole.toStringUtf8());
  }

  /**
   * enable etcd auth
   */
  @Test(dependsOnGroups = "user", groups = "authEnable", priority = 1)
  public void testEnableAuth() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient.authEnable().get();
  }

  /**
   * auth client with rootPass and user name
   */
  @Test(dependsOnMethods = "testEnableAuth", groups = "authEnable", priority = 1)
  public void setupAuthClient() {
    this.userClient = Client.builder()
        .endpoints(endpoints)
        .user(user)
        .password(userNewPass).build();
    this.rootClient = Client.builder()
        .endpoints(endpoints)
        .user(root)
        .password(rootPass).build();
  }

  /**
   * put and range with auth client
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable", priority = 1)
  public void testKVWithAuth() throws ExecutionException, InterruptedException {
    this.userClient.getKVClient().put(rootRoleKey, rootRoleValue).get();
    this.userClient.getKVClient().put(userRoleKey, userRoleValue).get();
    this.userClient.getKVClient().get(rootRoleKey).get();
    this.userClient.getKVClient().get(userRoleKey).get();
  }

  /**
   * put and range with non auth client
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable", priority = 1)
  public void testKVWithoutAuth() throws InterruptedException {
    assertThatThrownBy(() -> this.authDisabledKVClient.put(rootRoleKey, rootRoleValue).get())
        .hasMessageContaining("etcdserver: user name is empty");
    assertThatThrownBy(() -> this.authDisabledKVClient.put(userRoleKey, rootRoleValue).get())
        .hasMessageContaining("etcdserver: user name is empty");
    assertThatThrownBy(() -> this.authDisabledKVClient.get(rootRoleKey).get())
        .hasMessageContaining("etcdserver: user name is empty");
    assertThatThrownBy(() -> this.authDisabledKVClient.get(userRoleKey).get())
        .hasMessageContaining("etcdserver: user name is empty");
  }

  /**
   * get auth's permission
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable", priority = 1)
  public void testRoleGet() throws ExecutionException, InterruptedException {
    AuthRoleGetResponse roleGetResponse = this.userClient.getAuthClient()
        .roleGet(rootRole)
        .get();
    assertThat(roleGetResponse.getPermissions().size()).isNotEqualTo(0);

    roleGetResponse = this.userClient.getAuthClient()
        .roleGet(userRole)
        .get();
    assertThat(roleGetResponse.getPermissions().size()).isNotEqualTo(0);
  }

  @Test(groups = "testAuth", dependsOnMethods = "testKVWithAuth", priority = 1)
  public void testUserRevokeRole() throws ExecutionException, InterruptedException {
    this.rootClient.getAuthClient().userRevokeRole(user, rootRole).get();

    KV kvClient = this.userClient.getKVClient();
    // verify the access to root role is revoked for user.
    assertThatThrownBy(() -> kvClient.get(rootRoleKey).get()).isNotNull();
    // verify userRole is still valid.
    assertThat(kvClient.get(userRoleKey).get().getCount()).isNotEqualTo(0);
  }

  @Test(groups = "testAuth", dependsOnMethods = {"testUserRevokeRole", "testRoleGet"}, priority = 1)
  public void testRoleRevokePermission() throws ExecutionException, InterruptedException {
    this.rootClient.getAuthClient()
        .roleRevokePermission(userRole, userRolekeyRangeBegin, userRolekeyRangeEnd).get();

    // verify the access to foo is revoked for user.
    assertThatThrownBy(() -> this.userClient.getKVClient().get(userRoleKey).get()).isNotNull();
  }

  /**
   * disable etcd auth
   */
  @Test(dependsOnGroups = "testAuth", groups = "disableAuth", priority = 1)
  public void testDisableAuth() throws ExecutionException, InterruptedException {
    this.rootClient.getAuthClient().authDisable().get();
  }

  /**
   * delete user
   */
  @Test(dependsOnGroups = "disableAuth", groups = "clearEnv", priority = 1)
  public void delUser() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient.userDelete(root).get();
    this.authDisabledAuthClient.userDelete(user).get();
  }

  /**
   * delete role
   */
  @Test(dependsOnGroups = "disableAuth", groups = "clearEnv", priority = 1)
  public void delRole() throws ExecutionException, InterruptedException {
    this.authDisabledAuthClient.roleDelete(rootRole).get();
    this.authDisabledAuthClient.roleDelete(userRole).get();
  }

  @AfterTest
  public void tearDown() throws IOException {
    CLUSTER.close();
  }
}
