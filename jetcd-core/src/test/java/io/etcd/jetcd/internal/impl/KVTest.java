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

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
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
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * KV service test cases.
 */
public class KVTest {

  private static final EtcdCluster CLUSTER = EtcdClusterFactory.buildCluster("etcd-kv", 3 ,false);
  private static KV kvClient;

  private static final ByteSequence SAMPLE_KEY = ByteSequence.from("sample_key", Charsets.UTF_8);
  private static final ByteSequence SAMPLE_VALUE = ByteSequence.from("sample_value", Charsets.UTF_8);
  private static final ByteSequence SAMPLE_KEY_2 = ByteSequence.from("sample_key2", Charsets.UTF_8);
  private static final ByteSequence SAMPLE_VALUE_2 = ByteSequence.from("sample_value2", Charsets.UTF_8);
  private static final ByteSequence SAMPLE_KEY_3 = ByteSequence.from("sample_key3", Charsets.UTF_8);

  @BeforeClass
  public static void setUp() throws Exception {
    CLUSTER.start();

    kvClient = Client.builder().endpoints(CLUSTER.getClientEndpoints()).build().getKVClient();
  }

  @Test
  public void testPut() throws Exception {
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY, SAMPLE_VALUE);
    PutResponse response = feature.get();
    assertTrue(response.getHeader() != null);
    assertTrue(!response.hasPrevKv());
  }

  @Test
  public void testPutWithNotExistLease() throws ExecutionException, InterruptedException {
    PutOption option = PutOption.newBuilder().withLeaseId(99999).build();
    CompletableFuture<PutResponse> future = kvClient.put(SAMPLE_KEY, SAMPLE_VALUE, option);
    Assertions.assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> future.get())
        .withMessageEndingWith("etcdserver: requested lease not found");
  }

  @Test
  public void testGet() throws Exception {
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY_2, SAMPLE_VALUE_2);
    feature.get();
    CompletableFuture<GetResponse> getFeature = kvClient.get(SAMPLE_KEY_2);
    GetResponse response = getFeature.get();
    assertEquals(1, response.getKvs().size());
    assertEquals(SAMPLE_VALUE_2.toString(UTF_8), response.getKvs().get(0).getValue().toString(UTF_8));
    assertTrue(!response.isMore());
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
    assertEquals(1, response.getKvs().size());
    assertEquals(SAMPLE_VALUE.toString(UTF_8), response.getKvs().get(0).getValue().toString(UTF_8));
  }

  @Test
  public void testGetSortedPrefix() throws Exception {
    String prefix = TestUtil.randomString();
    int numPrefix = 3;
    putKeysWithPrefix(prefix, numPrefix);

    GetOption option = GetOption.newBuilder()
        .withSortField(SortTarget.KEY)
        .withSortOrder(SortOrder.DESCEND)
        .withPrefix(ByteSequence.from(prefix, Charsets.UTF_8))
        .build();
    CompletableFuture<GetResponse> getFeature = kvClient
        .get(ByteSequence.from(prefix, Charsets.UTF_8), option);
    GetResponse response = getFeature.get();

    assertEquals(numPrefix, response.getKvs().size());
    for (int i = 0; i < numPrefix; i++) {
      assertEquals(prefix + (numPrefix - i - 1), response.getKvs().get(i).getKey().toString(UTF_8));
      assertEquals(String.valueOf(numPrefix - i - 1), response.getKvs().get(i).getValue().toString(UTF_8));
    }
  }

  @Test
  public void testDelete() throws Exception {
    // Put content so that we actually have something to delete
    testPut();

    ByteSequence keyToDelete = SAMPLE_KEY;

    // count keys about to delete
    CompletableFuture<GetResponse> getFeature = kvClient.get(keyToDelete);
    GetResponse resp = getFeature.get();

    // delete the keys
    CompletableFuture<DeleteResponse> deleteFuture = kvClient.delete(keyToDelete);
    DeleteResponse delResp = deleteFuture.get();
    assertEquals(resp.getKvs().size(), delResp.getDeleted());
  }

  @Test
  public void testGetAndDeleteWithPrefix() throws Exception {
    String prefix = TestUtil.randomString();
    ByteSequence key = ByteSequence.from(prefix, Charsets.UTF_8);
    int numPrefixes = 10;

    putKeysWithPrefix(prefix, numPrefixes);

    // verify get withPrefix.
    CompletableFuture<GetResponse> getFuture = kvClient
        .get(key, GetOption.newBuilder().withPrefix(key).build());
    GetResponse getResp = getFuture.get();
    assertEquals(numPrefixes, getResp.getCount());

    // verify del withPrefix.
    DeleteOption deleteOpt = DeleteOption.newBuilder()
        .withPrefix(key).build();
    CompletableFuture<DeleteResponse> delFuture = kvClient.delete(key, deleteOpt);
    DeleteResponse delResp = delFuture.get();
    assertEquals(numPrefixes, delResp.getDeleted());
  }

  private static void putKeysWithPrefix(String prefix, int numPrefixes)
      throws ExecutionException, InterruptedException {
    for (int i = 0; i < numPrefixes; i++) {
      ByteSequence key = ByteSequence.from(prefix + i, Charsets.UTF_8);
      ByteSequence value = ByteSequence.from("" + i, Charsets.UTF_8);
      kvClient.put(key, value).get();
    }
  }

  @Test
  public void testTxn() throws Exception {
    ByteSequence sampleKey = ByteSequence.from("txn_key", Charsets.UTF_8);
    ByteSequence sampleValue = ByteSequence.from("xyz", Charsets.UTF_8);
    ByteSequence cmpValue = ByteSequence.from("abc", Charsets.UTF_8);
    ByteSequence putValue = ByteSequence.from("XYZ", Charsets.UTF_8);
    ByteSequence putValueNew = ByteSequence.from("ABC", Charsets.UTF_8);
    // put the original txn key value pair
    kvClient.put(sampleKey, sampleValue).get();

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
    assertEquals(1, getResp.getKvs().size());
    assertEquals(putValue.toString(UTF_8), getResp.getKvs().get(0).getValue().toString(UTF_8));
  }

  @Test
  public void testNestedTxn() throws Exception {
    ByteSequence foo = ByteSequence.from("txn_foo", Charsets.UTF_8);
    ByteSequence bar = ByteSequence.from("txn_bar", Charsets.UTF_8);
    ByteSequence barz = ByteSequence.from("txn_barz", Charsets.UTF_8);
    ByteSequence abc = ByteSequence.from("txn_abc", Charsets.UTF_8);
    ByteSequence oneTwoThree = ByteSequence.from("txn_123", Charsets.UTF_8);

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
    assertEquals(1, getResp.getKvs().size());
    assertEquals(bar.toString(UTF_8), getResp.getKvs().get(0).getValue().toString(UTF_8));

    GetResponse getResp2 = kvClient.get(abc).get();
    assertEquals(1, getResp2.getKvs().size());
    assertEquals(oneTwoThree.toString(UTF_8), getResp2.getKvs().get(0).getValue().toString(UTF_8));
  }

  @AfterClass
  public static void tearDown() throws IOException {
    CLUSTER.close();
  }
}
