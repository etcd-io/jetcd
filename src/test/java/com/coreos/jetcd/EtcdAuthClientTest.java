package com.coreos.jetcd;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import org.testng.annotations.*;
import org.testng.asserts.Assertion;

import com.coreos.jetcd.api.AuthRoleGetResponse;
import com.coreos.jetcd.api.Permission;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;


/**
 * test etcd auth
 */
public class EtcdAuthClientTest extends AbstractEtcdInstanceTest
{
    private EtcdAuth authClient;
    private EtcdKV kvClient;

    private ByteString roleName = ByteString.copyFromUtf8("root");

    private ByteString keyRangeBegin = ByteString.copyFromUtf8("foo");
    private ByteString keyRangeEnd = ByteString.copyFromUtf8("zoo");

    private ByteString testKey = ByteString.copyFromUtf8("foo1");
    private ByteString testName = ByteString.copyFromUtf8("bar");

    private ByteString userName = ByteString
            .copyFrom("root", Charset.defaultCharset());
    private ByteString password = ByteString
            .copyFrom("123", Charset.defaultCharset());

    private Assertion test;
    private EtcdClient authEtcdClient;


    /**
     * Build etcd client to create role, permission
     */
    @BeforeMethod
    public void setupEnv() throws AuthFailedException, ConnectException
    {
        this.test = new Assertion();
        final EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(etcdInstance.getEndpoint()).build();
        this.kvClient = etcdClient.getKVClient();
        this.authClient = etcdClient.getAuthClient();
    }

    /**
     * create role with un-auth etcd client
     */
    @Test(groups = "role")
    public void testRoleAdd() throws ExecutionException, InterruptedException
    {
        this.authClient.roleAdd(roleName).get();
    }

    /**
     * grant permission to role
     */
    @Test(dependsOnMethods = "testRoleAdd", groups = "role")
    public void testRoleGrantPermission() throws ExecutionException,
                                                 InterruptedException
    {
        this.authClient
                .roleGrantPermission(roleName, keyRangeBegin, keyRangeEnd,
                                     Permission.Type.READWRITE).get();
    }

    /**
     * add user with password and username
     */
    @Test(groups = "user")
    public void testUserAdd() throws ExecutionException, InterruptedException
    {
        this.authClient.userAdd(userName, password).get();
    }

    /**
     * grant user role
     */
    @Test(dependsOnMethods = {"testUserAdd",
                              "testRoleGrantPermission"}, groups = "user")
    public void testUserGrantRole() throws ExecutionException,
                                           InterruptedException
    {
        this.authClient.userGrantRole(userName, roleName).get();
    }

    /**
     * enable etcd auth
     */
    @Test(dependsOnGroups = "user", groups = "authEnable")
    public void testEnableAuth() throws ExecutionException, InterruptedException
    {
        this.authClient.authEnable().get();
    }

    /**
     * auth client with password and user name
     */
    @Test(dependsOnMethods = "testEnableAuth", groups = "authEnable")
    public void setupAuthClient() throws AuthFailedException, ConnectException
    {
        this.authEtcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(etcdInstance.getEndpoint()).setName(userName)
                .setPassword(password).build();

    }

    /**
     * put and range with auth client
     */
    @Test(groups = "testAuth", dependsOnGroups = "authEnable")
    public void testKVWithAuth() throws ExecutionException, InterruptedException
    {
        Throwable err = null;
        try
        {
            this.authEtcdClient.getKVClient().put(testKey, testName).get();
            RangeResponse rangeResponse = this.authEtcdClient.getKVClient()
                    .get(testKey).get();
            this.test.assertTrue(rangeResponse.getCount() != 0 && rangeResponse
                    .getKvs(0).getValue().equals(testName));
        }
        catch (StatusRuntimeException sre)
        {
            err = sre;
        }
        this.test.assertNull(err, "KV put range test with auth");
    }

    /**
     * put and range with auth client
     */
    @Test(groups = "testAuth", dependsOnGroups = "authEnable")
    public void testKVWithoutAuth() throws InterruptedException
    {
        Throwable err = null;
        try
        {
            this.kvClient.put(testKey, testName).get();
            this.kvClient.get(testKey).get();
        }
        catch (ExecutionException sre)
        {
            err = sre;
        }
        this.test.assertNotNull(err, "KV put range test without auth");
    }

    /**
     * get auth's permission
     */
    @Test(groups = "testAuth", dependsOnGroups = "authEnable")
    public void testRoleGet() throws ExecutionException, InterruptedException
    {
        AuthRoleGetResponse roleGetResponse = this.authEtcdClient
                .getAuthClient().roleGet(roleName).get();
        this.test.assertTrue(roleGetResponse.getPermCount() != 0);
    }

    /**
     * disable etcd auth
     */
    @Test(dependsOnGroups = "testAuth", groups = "disableAuth")
    public void testDisableAuth()
    {
        Throwable err = null;
        try
        {
            this.authEtcdClient.getAuthClient().authDisable().get();
        }
        catch (Exception e)
        {
            err = e;
        }
        this.test.assertNull(err, "auth disable");
    }

    /**
     * delete user
     */
    @Test(dependsOnGroups = "disableAuth", groups = "clearEnv")
    public void delUser()
    {
        Throwable err = null;
        try
        {
            this.authEtcdClient.getAuthClient().userDelete(userName).get();
        }
        catch (Exception e)
        {
            err = e;
        }
        this.test.assertNull(err, "user delete");
    }

    /**
     * delete role
     */
    @Test(dependsOnGroups = "disableAuth", groups = "clearEnv")
    public void delRole()
    {
        Throwable err = null;
        try
        {
            this.authEtcdClient.getAuthClient().roleDelete(roleName).get();
        }
        catch (Exception e)
        {
            err = e;
        }
        this.test.assertNull(err, "delete role");
    }

}
