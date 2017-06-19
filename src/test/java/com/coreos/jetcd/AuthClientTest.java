package com.coreos.jetcd;

import com.coreos.jetcd.api.AuthRoleGetResponse;
import com.coreos.jetcd.api.Permission;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.kv.GetResponse;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * test etcd auth
 */
public class AuthClientTest {

  private Auth authClient;
  private KV kvClient;

  private ByteSequence roleName = ByteSequence.fromString("root");

  private ByteSequence keyRangeBegin = ByteSequence.fromString("foo");
  private ByteSequence keyRangeEnd = ByteSequence.fromString("zoo");

  private ByteSequence testKey = ByteSequence.fromString("foo1");
  private ByteSequence testName = ByteSequence.fromString("bar1");

  private ByteSequence userName = ByteSequence.fromString("root");
  private ByteSequence password = ByteSequence.fromString("123");

  private Assertion test;

  private Client client;
  private Client secureClient;

  /**
   * Build etcd client to create role, permission
   */
  @BeforeTest
  public void setupEnv() throws AuthFailedException, ConnectException {
    this.test = new Assertion();
    this.client = ClientBuilder.newBuilder().endpoints("localhost:2379").build();
    this.kvClient = this.client.getKVClient();
    this.authClient = this.client.getAuthClient();
  }

  /**
   * create role with un-auth etcd client
   */
  @Test(groups = "role", priority = 1)
  public void testRoleAdd() throws ExecutionException, InterruptedException {
    this.authClient.roleAdd(roleName).get();
  }

  /**
   * grant permission to role
   */
  @Test(dependsOnMethods = "testRoleAdd", groups = "role", priority = 1)
  public void testRoleGrantPermission() throws ExecutionException, InterruptedException {
    this.authClient
        .roleGrantPermission(roleName, keyRangeBegin, keyRangeEnd, Permission.Type.READWRITE).get();
  }

  /**
   * add user with password and username
   */
  @Test(groups = "user", priority = 1)
  public void testUserAdd() throws ExecutionException, InterruptedException {
    this.authClient.userAdd(userName, password).get();
  }

  /**
   * grant user role
   */
  @Test(dependsOnMethods = {"testUserAdd",
      "testRoleGrantPermission"}, groups = "user", priority = 1)
  public void testUserGrantRole() throws ExecutionException, InterruptedException {
    this.authClient.userGrantRole(userName, roleName).get();
  }

  /**
   * enable etcd auth
   */
  @Test(dependsOnGroups = "user", groups = "authEnable", priority = 1)
  public void testEnableAuth() throws ExecutionException, InterruptedException {
    this.authClient.authEnable().get();
  }

  /**
   * auth client with password and user name
   */
  @Test(dependsOnMethods = "testEnableAuth", groups = "authEnable", priority = 1)
  public void setupAuthClient() throws AuthFailedException, ConnectException {
    this.secureClient = ClientBuilder.newBuilder().endpoints("localhost:2379")
        .setName(userName).setPassword(password).build();

  }

  /**
   * put and range with auth client
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable", priority = 1)
  public void testKVWithAuth() throws ExecutionException, InterruptedException {
    Throwable err = null;
    try {
      this.secureClient.getKVClient().put(testKey, testName).get();
      GetResponse getResponse = this.secureClient.getKVClient().get(testKey).get();
      this.test.assertTrue(getResponse.getCount() != 0);
      this.test.assertEquals(getResponse.getKvs().get(0).getValue().getBytes(), testName.getBytes());
    } catch (StatusRuntimeException sre) {
      err = sre;
    }
    this.test.assertNull(err, "KV put range test with auth");
  }

  /**
   * put and range with auth client
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable", priority = 1)
  public void testKVWithoutAuth() throws InterruptedException {
    Throwable err = null;
    try {
      this.kvClient.put(testKey, testName).get();
      this.kvClient.get(testKey).get();
    } catch (ExecutionException sre) {
      err = sre;
    }
    this.test.assertNotNull(err, "KV put range test without auth");
  }

  /**
   * get auth's permission
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable", priority = 1)
  public void testRoleGet() throws ExecutionException, InterruptedException {
    AuthRoleGetResponse roleGetResponse = this.secureClient.getAuthClient().roleGet(roleName)
        .get();
    this.test.assertTrue(roleGetResponse.getPermCount() != 0);
  }

  /**
   * disable etcd auth
   */
  @Test(dependsOnGroups = "testAuth", groups = "disableAuth", priority = 1)
  public void testDisableAuth() {
    Throwable err = null;
    try {
      this.secureClient.getAuthClient().authDisable().get();
    } catch (Exception e) {
      err = e;
    }
    this.test.assertNull(err, "auth disable");
  }

  /**
   * delete user
   */
  @Test(dependsOnGroups = "disableAuth", groups = "clearEnv", priority = 1)
  public void delUser() {
    Throwable err = null;
    try {
      this.client.getAuthClient().userDelete(userName).get();
    } catch (Exception e) {
      err = e;
    }
    this.test.assertNull(err, "user delete");
  }

  /**
   * delete role
   */
  @Test(dependsOnGroups = "disableAuth", groups = "clearEnv", priority = 1)
  public void delRole() {
    Throwable err = null;
    try {
      this.client.getAuthClient().roleDelete(roleName).get();
    } catch (Exception e) {
      err = e;
    }
    this.test.assertNull(err, "delete role");
  }
}
