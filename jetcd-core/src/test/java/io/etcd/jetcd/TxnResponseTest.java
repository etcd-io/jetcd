/*
 * Copyright 2016-2021 The jetcd authors
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

import io.etcd.jetcd.api.DeleteRangeResponse;
import io.etcd.jetcd.api.PutResponse;
import io.etcd.jetcd.api.RangeResponse;
import io.etcd.jetcd.api.ResponseOp;
import io.etcd.jetcd.kv.TxnResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TxnResponseTest {

    private TxnResponse txnResponse;

    @BeforeEach
    public void setUp() {
        io.etcd.jetcd.api.TxnResponse response = io.etcd.jetcd.api.TxnResponse.newBuilder()
            .addResponses(ResponseOp.newBuilder().setResponsePut(PutResponse.getDefaultInstance()))
            .addResponses(ResponseOp.newBuilder().setResponseDeleteRange(DeleteRangeResponse.getDefaultInstance()))
            .addResponses(ResponseOp.newBuilder().setResponseRange(RangeResponse.getDefaultInstance()))
            .addResponses(ResponseOp.newBuilder().setResponseTxn(io.etcd.jetcd.api.TxnResponse.getDefaultInstance()))
            .build();
        txnResponse = new TxnResponse(response, ByteSequence.EMPTY);
    }

    @Test
    public void getDeleteResponsesTest() {
        assertThat(txnResponse.getDeleteResponses().size()).isEqualTo(1);
    }

    @Test
    public void getPutResponsesTest() {
        assertThat(txnResponse.getPutResponses().size()).isEqualTo(1);
    }

    @Test
    public void getGetResponsesTest() {
        assertThat(txnResponse.getGetResponses().size()).isEqualTo(1);
    }

    @Test
    public void getTxnResponsesTest() {
        assertThat(txnResponse.getTxnResponses().size()).isEqualTo(1);
    }

}
