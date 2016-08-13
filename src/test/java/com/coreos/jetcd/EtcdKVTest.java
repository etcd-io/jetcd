package com.coreos.jetcd;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.op.Cmp;
import com.coreos.jetcd.op.CmpTarget;
import com.coreos.jetcd.op.Op;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

/**
 * KV service test cases.
 */
public class EtcdKVTest {

    private EtcdKV kvClient;
    private Assertion test;

    @BeforeTest
    public void setUp() throws Exception {
        test = new Assertion();
        EtcdClient client = EtcdClientBuilder.newBuilder().endpoints("http://localhost:2379").build();
        kvClient = client.getKVClient();
    }

    @Test
    public void testPut() throws Exception {
        ByteString sampleKey = ByteString.copyFrom("sample_key", "UTF-8");
        ByteString sampleValue = ByteString.copyFrom("sample_value", "UTF-8");
        ListenableFuture<PutResponse> feature = kvClient.put(sampleKey, sampleValue);
        try {
            PutResponse response = feature.get();
            test.assertTrue(response.hasHeader());
            test.assertTrue(!response.hasPrevKv());
        } catch (Exception e) {
            // empty
            e.printStackTrace();
        }
    }

    @Test
    public void testPutWithNotExistLease() throws Exception {
        ByteString sampleKey = ByteString.copyFrom("sample_key", "UTF-8");
        ByteString sampleValue = ByteString.copyFrom("sample_value", "UTF-8");
        PutOption option = PutOption.newBuilder().withLeaseId(99999).build();
        ListenableFuture<PutResponse> feature = kvClient.put(sampleKey, sampleValue, option);
        try {
            PutResponse response = feature.get();
            test.assertTrue(response.hasHeader());
        } catch (Exception e) {
            // empty
        }
    }

    @Test
    public void testGet() throws Exception {
        ByteString sampleKey = ByteString.copyFrom("sample_key2", "UTF-8");
        ByteString sampleValue = ByteString.copyFrom("sample_value2", "UTF-8");
        ListenableFuture<PutResponse> feature = kvClient.put(sampleKey, sampleValue);
        try {
            feature.get();
            ListenableFuture<RangeResponse> getFeature = kvClient.get(sampleKey);
            RangeResponse response = getFeature.get();
            test.assertEquals(response.getKvsCount(), 1);
            test.assertEquals(response.getKvs(0).getValue().toStringUtf8(), "sample_value2");
            test.assertTrue(!response.getMore());
        } catch (Exception e) {
            // empty
        }
    }

    @Test
    public void testGetWithRev() throws Exception {
        ByteString sampleKey = ByteString.copyFrom("sample_key3", "UTF-8");
        ByteString sampleValue = ByteString.copyFrom("sample_value", "UTF-8");
        ByteString sampleValueTwo = ByteString.copyFrom("sample_value2", "UTF-8");
        ListenableFuture<PutResponse> feature = kvClient.put(sampleKey, sampleValue);
        try {
            PutResponse putResp = feature.get();
            kvClient.put(sampleKey, sampleValueTwo).get();
            GetOption option = GetOption.newBuilder().withRevision(putResp.getHeader().getRevision()).build();
            ListenableFuture<RangeResponse> getFeature = kvClient.get(sampleKey, option);
            RangeResponse response = getFeature.get();
            test.assertEquals(response.getKvsCount(), 1);
            test.assertEquals(response.getKvs(0).getValue().toStringUtf8(), "sample_value");
        } catch (Exception e) {
            // empty
        }
    }

    @Test
    public void testGetSortedPrefix() throws Exception {
        ByteString key = ByteString.copyFrom("test_key", "UTF-8");
        ByteString testValue = ByteString.copyFrom("test_value", "UTF-8");
        for (int i = 0; i < 3; i++) {
            ByteString testKey = ByteString.copyFrom("test_key" + i, "UTF-8");
            try {
                kvClient.put(testKey, testValue).get();
            } catch (Exception e) {
                // empty
            }
        }
        ByteString endKey = ByteString.copyFrom("\0", "UTF-8");
        GetOption option = GetOption.newBuilder().withSortField(RangeRequest.SortTarget.KEY).withSortOrder(RangeRequest.SortOrder.DESCEND)
                .withRange(endKey).build();
        try {
            ListenableFuture<RangeResponse> getFeature = kvClient.get(key, option);
            RangeResponse response = getFeature.get();
            test.assertEquals(response.getKvsCount(), 3);
            test.assertEquals(response.getKvs(0).getKey().toStringUtf8(), "test_key2");
            test.assertEquals(response.getKvs(0).getValue().toStringUtf8(), "test_value");
            test.assertEquals(response.getKvs(1).getKey().toStringUtf8(), "test_key1");
            test.assertEquals(response.getKvs(1).getValue().toStringUtf8(), "test_value");
            test.assertEquals(response.getKvs(2).getKey().toStringUtf8(), "test_key0");
            test.assertEquals(response.getKvs(2).getValue().toStringUtf8(), "test_value");
        } catch (Exception e) {
            // empty
        }
    }

    @Test(dependsOnMethods = "testPut")
    public void testDelete() throws Exception {
        ByteString keyToDelete = ByteString.copyFrom("sample_key", "UTF-8");
        try {
            // count keys about to delete
            ListenableFuture<RangeResponse> getFeature = kvClient.get(keyToDelete);
            RangeResponse resp = getFeature.get();

            // delete the keys
            ListenableFuture<DeleteRangeResponse> deleteFuture = kvClient.delete(keyToDelete);
            DeleteRangeResponse delResp = deleteFuture.get();
            test.assertEquals(resp.getKvsList().size(), delResp.getDeleted());
        } catch (Exception e) {
            // empty
        }
    }

    @Test
    public void testTxn() throws Exception {
        ByteString sampleKey = ByteString.copyFrom("txn_key", "UTF-8");
        ByteString sampleValue = ByteString.copyFrom("xyz", "UTF-8");
        ByteString cmpValue = ByteString.copyFrom("abc", "UTF-8");
        ByteString putValue = ByteString.copyFrom("XYZ", "UTF-8");
        ByteString putValueNew = ByteString.copyFrom("ABC", "UTF-8");
        ListenableFuture<PutResponse> feature = kvClient.put(sampleKey, sampleValue);
        try {
            // put the original txn key value pair
            PutResponse putResp = feature.get();

            // construct txn operation
            Cmp cmp = new Cmp(sampleKey, Cmp.Op.GREATER, CmpTarget.value(cmpValue));
            Txn txn = Txn.newBuilder().If(cmp).Then(Op.put(sampleKey, putValue, PutOption.DEFAULT))
                    .Else(Op.put(sampleKey, putValueNew, PutOption.DEFAULT)).build();
            ListenableFuture<TxnResponse> txnResp = kvClient.commit(txn);
            txnResp.get();

            // get the value
            RangeResponse getResp = kvClient.get(sampleKey).get();
            test.assertEquals(getResp.getKvsList().size(), 1);
            test.assertEquals(getResp.getKvs(0).getValue().toStringUtf8(), "XYZ");
        } catch (Exception e) {
            // empty
        }
    }
}
