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

package io.etcd.jetcd.kv;

import java.util.List;
import java.util.function.Supplier;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.common.suppliers.Suppliers;
import io.etcd.jetcd.impl.AbstractResponse;

import static io.etcd.jetcd.api.ResponseOp.ResponseCase.RESPONSE_DELETE_RANGE;
import static io.etcd.jetcd.api.ResponseOp.ResponseCase.RESPONSE_PUT;
import static io.etcd.jetcd.api.ResponseOp.ResponseCase.RESPONSE_RANGE;
import static io.etcd.jetcd.api.ResponseOp.ResponseCase.RESPONSE_TXN;

/**
 * TxnResponse returned by a transaction call contains lists of put, get, delete responses
 * corresponding to either the compare in txn.IF is evaluated to true or false.
 */
public class TxnResponse extends AbstractResponse<io.etcd.jetcd.api.TxnResponse> {

    private final Supplier<List<PutResponse>> putResponses;
    private final Supplier<List<GetResponse>> getResponses;
    private final Supplier<List<DeleteResponse>> deleteResponses;
    private final Supplier<List<TxnResponse>> txnResponses;

    /**
     * Creates a new TxnResponse from the gRPC response.
     *
     * @param txnResponse the gRPC transaction response
     * @param namespace   the namespace
     */
    public TxnResponse(io.etcd.jetcd.api.TxnResponse txnResponse, ByteSequence namespace) {
        super(txnResponse, txnResponse.getHeader());

        this.deleteResponses = Suppliers.memoizing(
            () -> getResponse().getResponsesList().stream()
                .filter((responseOp) -> responseOp.getResponseCase() == RESPONSE_DELETE_RANGE)
                .map(responseOp -> new DeleteResponse(responseOp.getResponseDeleteRange(), namespace))
                .toList());

        this.getResponses = Suppliers.memoizing(
            () -> getResponse().getResponsesList().stream()
                .filter((responseOp) -> responseOp.getResponseCase() == RESPONSE_RANGE)
                .map(responseOp -> new GetResponse(responseOp.getResponseRange(), namespace))
                .toList());

        this.putResponses = Suppliers.memoizing(
            () -> getResponse().getResponsesList().stream()
                .filter((responseOp) -> responseOp.getResponseCase() == RESPONSE_PUT)
                .map(responseOp -> new PutResponse(responseOp.getResponsePut(), namespace))
                .toList());

        this.txnResponses = Suppliers.memoizing(
            () -> getResponse().getResponsesList().stream()
                .filter((responseOp) -> responseOp.getResponseCase() == RESPONSE_TXN)
                .map(responseOp -> new TxnResponse(responseOp.getResponseTxn(), namespace))
                .toList());
    }

    /**
     * Returns true if the compare evaluated to true or false otherwise.
     *
     * @return if succeeded.
     */
    public boolean isSucceeded() {
        return getResponse().getSucceeded();
    }

    /**
     * Returns a list of DeleteResponse; empty list if none.
     *
     * @return delete responses.
     */
    public List<DeleteResponse> getDeleteResponses() {
        return deleteResponses.get();
    }

    /**
     * Returns a list of GetResponse; empty list if none.
     *
     * @return get responses.
     */
    public List<GetResponse> getGetResponses() {
        return getResponses.get();
    }

    /**
     * Returns a list of PutResponse; empty list if none.
     *
     * @return put responses.
     */
    public List<PutResponse> getPutResponses() {
        return putResponses.get();
    }

    /**
     * Returns a list of TxnResponse; empty list if none.
     *
     * @return txn responses.
     */
    public List<TxnResponse> getTxnResponses() {
        return txnResponses.get();
    }
}
