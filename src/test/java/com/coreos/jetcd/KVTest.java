package com.coreos.jetcd;

import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.api.RangeRequest;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.TxnResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.op.Cmp;
import com.coreos.jetcd.op.CmpTarget;
import com.coreos.jetcd.op.Op;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;


/**
 * KV service test cases.
 */
public class KVTest {

  private KV kvClient;
  private Assertion test;

  private static final ByteSequence SAMPLE_KEY = ByteSequence.fromString("sample_key");
  private static final ByteSequence SAMPLE_VALUE = ByteSequence.fromString("sample_value");
  private static final ByteSequence SAMPLE_KEY_2 = ByteSequence.fromString("sample_key2");
  private static final ByteSequence SAMPLE_VALUE_2 = ByteSequence.fromString("sample_value2");
  private static final ByteSequence SAMPLE_KEY_3 = ByteSequence.fromString("sample_key3");


  @BeforeTest
  public void setUp() throws Exception {
    test = new Assertion();
    Client client = ClientBuilder.newBuilder().endpoints(TestConstants.endpoints).build();
    kvClient = client.getKVClient();
  }

  @Test
  public void testPut() throws Exception {
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY, SAMPLE_VALUE);
    PutResponse response = feature.get();
    test.assertTrue(response.hasHeader());
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
    CompletableFuture<RangeResponse> getFeature = kvClient.get(SAMPLE_KEY_2);
    RangeResponse response = getFeature.get();
    test.assertEquals(response.getKvsCount(), 1);
    test.assertEquals(response.getKvs(0).getValue().toStringUtf8(),
        SAMPLE_VALUE_2.toStringUtf8());
    test.assertTrue(!response.getMore());
  }

  @Test
  public void testGetWithRev() throws Exception {
    CompletableFuture<PutResponse> feature = kvClient.put(SAMPLE_KEY_3, SAMPLE_VALUE);
    PutResponse putResp = feature.get();
    kvClient.put(SAMPLE_KEY_3, SAMPLE_VALUE_2).get();
    GetOption option = GetOption.newBuilder().withRevision(putResp.getHeader().getRevision())
        .build();
    CompletableFuture<RangeResponse> getFeature = kvClient.get(SAMPLE_KEY_3, option);
    RangeResponse response = getFeature.get();
    test.assertEquals(response.getKvsCount(), 1);
    test.assertEquals(response.getKvs(0).getValue().toStringUtf8(), SAMPLE_VALUE.toStringUtf8());
  }

  @Test
  public void testGetSortedPrefix() throws Exception {
    String prefix = randomString();
    int numPrefix = 3;
    putKeysWithPrefix(prefix, numPrefix);

    GetOption option = GetOption.newBuilder().withSortField(RangeRequest.SortTarget.KEY)
        .withSortOrder(RangeRequest.SortOrder.DESCEND)
        .withPrefix(ByteSequence.fromString(prefix))
        .build();
    CompletableFuture<RangeResponse> getFeature = kvClient
        .get(ByteSequence.fromString(prefix), option);
    RangeResponse response = getFeature.get();

    test.assertEquals(response.getKvsCount(), numPrefix);
    for (int i = 0; i < numPrefix; i++) {
      test.assertEquals(response.getKvs(i).getKey().toStringUtf8(), prefix + (numPrefix - i - 1));
      test.assertEquals(response.getKvs(i).getValue().toStringUtf8(),
          String.valueOf(numPrefix - i - 1));
    }
  }

  @Test(dependsOnMethods = "testPut")
  public void testDelete() throws Exception {
    ByteSequence keyToDelete = SAMPLE_KEY;

    // count keys about to delete
    CompletableFuture<RangeResponse> getFeature = kvClient.get(keyToDelete);
    RangeResponse resp = getFeature.get();

    // delete the keys
    CompletableFuture<DeleteRangeResponse> deleteFuture = kvClient.delete(keyToDelete);
    DeleteRangeResponse delResp = deleteFuture.get();
    test.assertEquals(resp.getKvsList().size(), delResp.getDeleted());
  }

  @Test
  public void testGetAndDeleteWithPrefix() throws Exception {
    String prefix = randomString();
    ByteSequence key = ByteSequence.fromString(prefix);
    int numPrefixes = 10;

    putKeysWithPrefix(prefix, numPrefixes);

    // verify get withPrefix.
    CompletableFuture<RangeResponse> getFuture = kvClient
        .get(key, GetOption.newBuilder().withPrefix(key).build());
    RangeResponse getResp = getFuture.get();
    test.assertEquals(getResp.getCount(), numPrefixes);

    // verify del withPrefix.
    DeleteOption deleteOpt = DeleteOption.newBuilder()
        .withPrefix(key).build();
    CompletableFuture<DeleteRangeResponse> delFuture = kvClient.delete(key, deleteOpt);
    DeleteRangeResponse delResp = delFuture.get();
    test.assertEquals(delResp.getDeleted(), numPrefixes);
  }

  String randomString() {
    return java.util.UUID.randomUUID().toString();
  }

  private void putKeysWithPrefix(String prefix, int numPrefixes)
      throws ExecutionException, InterruptedException {
    for (int i = 0; i < numPrefixes; i++) {
      ByteSequence key = ByteSequence.fromString(prefix + i);
      ByteSequence value = ByteSequence.fromString("" + i);
      kvClient.put(key, value).get();
    }
  }

  @Test
  public void testTxn() throws Exception {
    ByteSequence sampleKey = ByteSequence.fromString("txn_key");
    ByteSequence sampleValue = ByteSequence.fromString("xyz");
    ByteSequence cmpValue = ByteSequence.fromString("abc");
    ByteSequence putValue = ByteSequence.fromString("XYZ");
    ByteSequence putValueNew = ByteSequence.fromString("ABC");
    CompletableFuture<PutResponse> feature = kvClient.put(sampleKey, sampleValue);
    // put the original txn key value pair
    PutResponse putResp = feature.get();

    // construct txn operation
    Cmp cmp = new Cmp(sampleKey, Cmp.Op.GREATER, CmpTarget.value(cmpValue));
    Txn txn = Txn.newBuilder().If(cmp).Then(Op.put(sampleKey, putValue, PutOption.DEFAULT))
        .Else(Op.put(sampleKey, putValueNew, PutOption.DEFAULT)).build();
    CompletableFuture<TxnResponse> txnResp = kvClient.commit(txn);
    txnResp.get();

    // get the value
    RangeResponse getResp = kvClient.get(sampleKey).get();
    test.assertEquals(getResp.getKvsList().size(), 1);
    test.assertEquals(getResp.getKvs(0).getValue().toStringUtf8(), putValue.toStringUtf8());
  }
}
