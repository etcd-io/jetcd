package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.op.Cmp;
import com.coreos.jetcd.op.CmpTarget;
import com.coreos.jetcd.op.Op;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

import java.util.concurrent.ExecutionException;

/**
 * KV service test cases.
 */
public class EtcdLeaseTest {

  private EtcdKV kvClient;
  private EtcdClient etcdClient;
  private EtcdLease leaseClient;
  private Assertion test;


  private ByteString testKey = ByteString.copyFromUtf8("foo1");
  private ByteString testName = ByteString.copyFromUtf8("bar");

  @BeforeTest
  public void setUp() throws Exception {
    test = new Assertion();
    etcdClient = EtcdClientBuilder.newBuilder().endpoints(TestConstants.endpoints).build();
    kvClient = etcdClient.getKVClient();

    leaseClient = etcdClient.getLeaseClient();
  }

  @Test
  public void testGrant() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();
    PutResponse putRep = kvClient
        .put(testKey, testName, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    test.assertEquals(kvClient.get(testKey).get().getCount(), 1);

    Thread.sleep(6000);
    test.assertEquals(kvClient.get(testKey).get().getCount(), 0);
  }

  @Test(dependsOnMethods = "testGrant")
  public void testRevoke() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();
    PutResponse putRep = kvClient
        .put(testKey, testName, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    test.assertEquals(kvClient.get(testKey).get().getCount(), 1);
    leaseClient.revoke(leaseID).get();
    test.assertEquals(kvClient.get(testKey).get().getCount(), 0);
  }

  @Test(dependsOnMethods = "testRevoke")
  public void testkeepAlive() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();
    PutResponse putRep = kvClient
        .put(testKey, testName, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    test.assertEquals(kvClient.get(testKey).get().getCount(), 1);
    leaseClient.startKeepAliveService();
    leaseClient.keepAlive(leaseID, new EtcdLease.EtcdLeaseHandler() {
      @Override
      public void onKeepAliveRespond(LeaseKeepAliveResponse keepAliveResponse) {

      }

      @Override
      public void onLeaseExpired(long leaseId) {

      }

      @Override
      public void onError(Throwable throwable) {

      }
    });
    Thread.sleep(6000);
    test.assertEquals(kvClient.get(testKey).get().getCount(), 1);
    leaseClient.cancelKeepAlive(leaseID);
    test.assertEquals(kvClient.get(testKey).get().getCount(), 0);
  }
}
