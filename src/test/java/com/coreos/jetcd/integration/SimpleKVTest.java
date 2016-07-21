package com.coreos.jetcd.integration;

import com.google.protobuf.ByteString;

import com.coreos.jetcd.api.DeleteRangeRequest;
import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.KVGrpc.KVBlockingStub;
import com.coreos.jetcd.api.PutRequest;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.api.RangeRequest;
import com.coreos.jetcd.api.RangeResponse;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleKVTest extends AbstractEtcdIntegrationTest {

    private KVBlockingStub kvBlockingStub;

    @BeforeTest
    public void preparStub() throws Exception {
        kvBlockingStub = prepareEtcdNativeClient().newKVBlockingStub();
    }

    @Test
    public void testBasicOperations() {
        final String key = "jetcd";
        final String value = "ongoing";

        // delete
        DeleteRangeResponse deleteRangeResponse = kvBlockingStub.deleteRange(
                DeleteRangeRequest.newBuilder().setKey(ByteString.copyFromUtf8(key)).build());
        System.out.println("deleteRangeResponse=" + deleteRangeResponse.toString());

        // put
        PutResponse putResponse = kvBlockingStub.put(PutRequest.newBuilder().setKey(ByteString.copyFromUtf8
                (key)).setValue(ByteString.copyFromUtf8
                (value)).build());
        System.out.println("putResponse=" + putResponse.toString());

        // get
        RangeResponse response = kvBlockingStub.range(RangeRequest.newBuilder().setKey(ByteString.copyFromUtf8
                (key)).build());
        System.out.println("RangeResponse=" + response.toString());

        assertThat(response.getKvsCount()).isEqualTo(1);
        assertThat(response.getKvs(0).getKey()).isEqualTo(ByteString.copyFromUtf8(key));
        assertThat(response.getKvs(0).getValue()).isEqualTo(ByteString.copyFromUtf8(value));
    }
}