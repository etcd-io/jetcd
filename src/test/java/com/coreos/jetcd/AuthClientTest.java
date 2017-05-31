package com.coreos.jetcd;

import com.coreos.jetcd.api.AuthRoleGetResponse;
import com.coreos.jetcd.api.Permission;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import java.nio.charset.Charset;
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

  private ByteString roleName = ByteString.copyFromUtf8("root");

  private ByteString keyRangeBegin = ByteString.copyFromUtf8("foo");
  private ByteString keyRangeEnd = ByteString.copyFromUtf8("zoo");

  private ByteString testKey = ByteString.copyFromUtf8("foo1");
  private ByteString testName = ByteString.copyFromUtf8("bar");

  private ByteString userName = ByteString.copyFrom("root", Charset.defaultCharset());
  private ByteString password = ByteString.copyFrom("123", Charset.defaultCharset());

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
  @Test(groups = "role")
  public void testRoleAdd() throws ExecutionException, InterruptedException {
    this.authClient.roleAdd(roleName).get();
  }

  /**
   * grant permission to role
   */
  @Test(dependsOnMethods = "testRoleAdd", groups = "role")
  public void testRoleGrantPermission() throws ExecutionException, InterruptedException {
    this.authClient
        .roleGrantPermission(roleName, keyRangeBegin, keyRangeEnd, Permission.Type.READWRITE).get();
  }

  /**
   * add user with password and username
   */
  @Test(groups = "user")
  public void testUserAdd() throws ExecutionException, InterruptedException {
    this.authClient.userAdd(userName, password).get();
  }

  /**
   * grant user role
   */
  @Test(dependsOnMethods = {"testUserAdd", "testRoleGrantPermission"}, groups = "user")
  public void testUserGrantRole() throws ExecutionException, InterruptedException {
    this.authClient.userGrantRole(userName, roleName).get();
  }

  /**
   * enable etcd auth
   */
  @Test(dependsOnGroups = "user", groups = "authEnable")
  public void testEnableAuth() throws ExecutionException, InterruptedException {
    this.authClient.authEnable().get();
  }

  /**
   * auth client with password and user name
   */
  @Test(dependsOnMethods = "testEnableAuth", groups = "authEnable")
  public void setupAuthClient() throws AuthFailedException, ConnectException {
    this.secureClient = ClientBuilder.newBuilder().endpoints("localhost:2379")
        .setName(userName).setPassword(password).build();

  }

  /**
   * put and range with auth client
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable")
  public void testKVWithAuth() throws ExecutionException, InterruptedException {
    Throwable err = null;
    try {
      this.secureClient.getKVClient().put(testKey, testName).get();
      RangeResponse rangeResponse = this.secureClient.getKVClient().get(testKey).get();
      this.test.assertTrue(
          rangeResponse.getCount() != 0 && rangeResponse.getKvs(0).getValue().equals(testName));
    } catch (StatusRuntimeException sre) {
      err = sre;
    }
    this.test.assertNull(err, "KV put range test with auth");
  }

  /**
   * put and range with auth client
   */
  @Test(groups = "testAuth", dependsOnGroups = "authEnable")
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
  @Test(groups = "testAuth", dependsOnGroups = "authEnable")
  public void testRoleGet() throws ExecutionException, InterruptedException {
    AuthRoleGetResponse roleGetResponse = this.secureClient.getAuthClient().roleGet(roleName)
        .get();
    this.test.assertTrue(roleGetResponse.getPermCount() != 0);
  }

  /**
   * disable etcd auth
   */
  @Test(dependsOnGroups = "testAuth", groups = "disableAuth")
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
  @Test(dependsOnGroups = "disableAuth", groups = "clearEnv")
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
  @Test(dependsOnGroups = "disableAuth", groups = "clearEnv")
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
