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

import io.etcd.jetcd.KV;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.internal.infrastructure.EtcdCluster;
import io.etcd.jetcd.internal.infrastructure.EtcdClusterFactory;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.GetOption.SortOrder;
import io.etcd.jetcd.options.GetOption.SortTarget;
import io.etcd.jetcd.options.PutOption;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;


/**
 * KV service test cases.
 */
public class KVTest {
  private static final EtcdCluster CLUSTER = EtcdClusterFactory.buildCluster("etcd-kv", 3 ,false);

  private KV kvClient;
  private Assertion test;

  private static final ByteSequence SAMPLE_KEY = ByteSequence.from("sample_key");
  private static final ByteSequence SAMPLE_VALUE = ByteSequence.from("sample_value");
  private static final ByteSequence SAMPLE_KEY_2 = ByteSequence.from("sample_key2");
  private static final ByteSequence SAMPLE_VALUE_2 = ByteSequence.from("sample_value2");
  private static final ByteSequence SAMPLE_KEY_3 = ByteSequence.from("sample_key3");


  @BeforeTest
  public void setUp() throws Exception {
    CLUSTER.start();

    test = new Assertion();
    kvClient = CLUSTER.getClient().getKVClient();
  }

  @Test
  public void testPut() throws Exception {
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY, SAMPLE_VALUE);
    PutResponse response = feature.get();
    test.assertTrue(response.getHeader() != null);
    test.assertTrue(!response.hasPrevKv());
  }

  @Test(expectedExceptions = ExecutionException.class,
      expectedExceptionsMessageRegExp = ".*etcdserver: requested lease not found")
  public void testPutWithNotExistLease() throws ExecutionException, InterruptedException {
    PutOption option = PutOption.newBuilder().withLeaseId(99999).build();
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY, SAMPLE_VALUE, option);
    feature.get();
  }

  @Test
  public void testGet() throws Exception {
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY_2, SAMPLE_VALUE_2);
    feature.get();
    CompletableFuture<GetResponse> getFeature = kvClient.get(SAMPLE_KEY_2);
    GetResponse response = getFeature.get();
    test.assertEquals(response.getKvs().size(), 1);
    test.assertEquals(response.getKvs().get(0).getValue().toStringUtf8(),
        SAMPLE_VALUE_2.toStringUtf8());
    test.assertTrue(!response.isMore());
  }

  @Test
  public void testGetWithRev() throws Exception {
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY_3, SAMPLE_VALUE);
    PutResponse putResp = feature.get();
    kvClient.put(SAMPLE_KEY_3, SAMPLE_VALUE_2).get();
    GetOption option = GetOption.newBuilder().withRevision(putResp.getHeader().getRevision())
        .build();
    CompletableFuture<GetResponse> getFeature = kvClient.get(SAMPLE_KEY_3, option);
    GetResponse response = getFeature.get();
    test.assertEquals(response.getKvs().size(), 1);
    test.assertEquals(response.getKvs().get(0).getValue().toStringUtf8(),
        SAMPLE_VALUE.toStringUtf8());
  }

  @Test
  public void testGetSortedPrefix() throws Exception {
    String prefix = TestUtil.randomString();
    int numPrefix = 3;
    putKeysWithPrefix(prefix, numPrefix);

    GetOption option = GetOption.newBuilder()
        .withSortField(SortTarget.KEY)
        .withSortOrder(SortOrder.DESCEND)
        .withPrefix(ByteSequence.from(prefix))
        .build();
    CompletableFuture<GetResponse> getFeature = kvClient
        .get(ByteSequence.from(prefix), option);
    GetResponse response = getFeature.get();

    test.assertEquals(response.getKvs().size(), numPrefix);
    for (int i = 0; i < numPrefix; i++) {
      test.assertEquals(response.getKvs().get(i).getKey().toStringUtf8(),
          prefix + (numPrefix - i - 1));
      test.assertEquals(response.getKvs().get(i).getValue().toStringUtf8(),
          String.valueOf(numPrefix - i - 1));
    }
  }

  @Test(dependsOnMethods = "testPut")
  public void testDelete() throws Exception {
    ByteSequence keyToDelete = SAMPLE_KEY;

    // count keys about to delete
    CompletableFuture<GetResponse> getFeature = kvClient.get(keyToDelete);
    GetResponse resp = getFeature.get();

    // delete the keys
    CompletableFuture<DeleteResponse> deleteFuture = kvClient.delete(keyToDelete);
    DeleteResponse delResp = deleteFuture.get();
    test.assertEquals(resp.getKvs().size(), delResp.getDeleted());
  }

  @Test
  public void testGetAndDeleteWithPrefix() throws Exception {
    String prefix = TestUtil.randomString();
    ByteSequence key = ByteSequence.from(prefix);
    int numPrefixes = 10;

    putKeysWithPrefix(prefix, numPrefixes);

    // verify get withPrefix.
    CompletableFuture<GetResponse> getFuture = kvClient
        .get(key, GetOption.newBuilder().withPrefix(key).build());
    GetResponse getResp = getFuture.get();
    test.assertEquals(getResp.getCount(), numPrefixes);

    // verify del withPrefix.
    DeleteOption deleteOpt = DeleteOption.newBuilder()
        .withPrefix(key).build();
    CompletableFuture<DeleteResponse> delFuture = kvClient.delete(key, deleteOpt);
    DeleteResponse delResp = delFuture.get();
    test.assertEquals(delResp.getDeleted(), numPrefixes);
  }

  private void putKeysWithPrefix(String prefix, int numPrefixes)
      throws ExecutionException, InterruptedException {
    for (int i = 0; i < numPrefixes; i++) {
      ByteSequence key = ByteSequence.from(prefix + i);
      ByteSequence value = ByteSequence.from("" + i);
      kvClient.put(key, value).get();
    }
  }

  @Test
  public void testTxn() throws Exception {
    ByteSequence sampleKey = ByteSequence.from("txn_key");
    ByteSequence sampleValue = ByteSequence.from("xyz");
    ByteSequence cmpValue = ByteSequence.from("abc");
    ByteSequence putValue = ByteSequence.from("XYZ");
    ByteSequence putValueNew = ByteSequence.from("ABC");
    CompletableFuture<PutResponse> feature = kvClient.put(sampleKey, sampleValue);
    // put the original txn key value pair
    PutResponse putResp = feature.get();

    // construct txn operation
    Txn txn = kvClient.txn();
    Cmp cmp = new Cmp(sampleKey, Cmp.Op.GREATER, CmpTarget.value(cmpValue));
    CompletableFuture<io.etcd.jetcd.kv.TxnResponse> txnResp =
        txn.If(cmp)
            .Then(Op.put(sampleKey, putValue, PutOption.DEFAULT))
            .Else(Op.put(sampleKey, putValueNew, PutOption.DEFAULT)).commit();
    txnResp.get();
    // get the value
    GetResponse getResp = kvClient.get(sampleKey).get();
    test.assertEquals(getResp.getKvs().size(), 1);
    test.assertEquals(getResp.getKvs().get(0).getValue().toStringUtf8(), putValue.toStringUtf8());
  }

  @Test
  public void testNestedTxn() throws Exception {
    ByteSequence foo = ByteSequence.from("txn_foo");
    ByteSequence bar = ByteSequence.from("txn_bar");
    ByteSequence barz = ByteSequence.from("txn_barz");
    ByteSequence abc = ByteSequence.from("txn_abc");
    ByteSequence oneTwoThree = ByteSequence.from("txn_123");
    
    Txn txn = kvClient.txn();
    Cmp cmp = new Cmp(foo, Cmp.Op.EQUAL, CmpTarget.version(0));
    CompletableFuture<io.etcd.jetcd.kv.TxnResponse> txnResp = txn.If(cmp)
        .Then(Op.put(foo, bar, PutOption.DEFAULT),
            Op.txn(null,
                new Op[] {Op.put(abc, oneTwoThree, PutOption.DEFAULT)},
                null))
        .Else(Op.put(foo, barz, PutOption.DEFAULT)).commit();
    txnResp.get();

    GetResponse getResp = kvClient.get(foo).get();
    test.assertEquals(getResp.getKvs().size(), 1);
    test.assertEquals(getResp.getKvs().get(0).getValue().toStringUtf8(), bar.toStringUtf8());

    GetResponse getResp2 = kvClient.get(abc).get();
    test.assertEquals(getResp2.getKvs().size(), 1);
    test.assertEquals(getResp2.getKvs().get(0).getValue().toStringUtf8(), oneTwoThree.toStringUtf8());
  }

  @AfterTest
  public void tearDown() throws IOException {
    CLUSTER.close();
  }
}
