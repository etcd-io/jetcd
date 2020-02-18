/*
 * Copyright 2016-2020 The jetcd authors
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

package io.etcd.jetcd;

import static com.google.common.base.Charsets.UTF_8;
import static io.etcd.jetcd.TestUtil.bytesOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.launcher.junit5.EtcdClusterExtension;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.GetOption.SortOrder;
import io.etcd.jetcd.options.GetOption.SortTarget;
import io.etcd.jetcd.options.PutOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class KVTest {

  @RegisterExtension
  public static EtcdClusterExtension cluster = new EtcdClusterExtension("etcd-kv", 3 ,false);
  private static KV kvClient;

  private static final ByteSequence SAMPLE_KEY = bytesOf("sample_key");
  private static final ByteSequence SAMPLE_VALUE = bytesOf("sample_value");
  private static final ByteSequence SAMPLE_KEY_2 = bytesOf("sample_key2");
  private static final ByteSequence SAMPLE_VALUE_2 = bytesOf("sample_value2");
  private static final ByteSequence SAMPLE_KEY_3 = bytesOf("sample_key3");

  @BeforeAll
  public static void setUp() throws Exception {
    kvClient = Client.builder().endpoints(cluster.getClientEndpoints()).build().getKVClient();
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
        .withPrefix(bytesOf(prefix))
        .build();
    CompletableFuture<GetResponse> getFeature = kvClient
        .get(bytesOf(prefix), option);
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
    ByteSequence key = bytesOf(prefix);
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
      ByteSequence key = bytesOf(prefix + i);
      ByteSequence value = bytesOf("" + i);
      kvClient.put(key, value).get();
    }
  }

  @Test
  public void testTxn() throws Exception {
    ByteSequence sampleKey = bytesOf("txn_key");
    ByteSequence sampleValue = bytesOf("xyz");
    ByteSequence cmpValue = bytesOf("abc");
    ByteSequence putValue = bytesOf("XYZ");
    ByteSequence putValueNew = bytesOf("ABC");
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
    ByteSequence foo = bytesOf("txn_foo");
    ByteSequence bar = bytesOf("txn_bar");
    ByteSequence barz = bytesOf("txn_barz");
    ByteSequence abc = bytesOf("txn_abc");
    ByteSequence oneTwoThree = bytesOf("txn_123");

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
}
