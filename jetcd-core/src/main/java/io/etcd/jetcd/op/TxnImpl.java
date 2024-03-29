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

package io.etcd.jetcd.op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.api.TxnRequest;
import io.etcd.jetcd.kv.TxnResponse;

import com.google.common.annotations.VisibleForTesting;

/**
 * Build an etcd transaction.
 */
public class TxnImpl implements Txn {

    public static TxnImpl newTxn(Function<TxnRequest, CompletableFuture<TxnResponse>> f, ByteSequence namespace) {
        return new TxnImpl(f, namespace);
    }

    @VisibleForTesting
    static TxnImpl newTxn(Function<TxnRequest, CompletableFuture<TxnResponse>> f) {
        return newTxn(f, ByteSequence.EMPTY);
    }

    private final ByteSequence namespace;

    private final List<Cmp> cmpList = new ArrayList<>();
    private final List<Op> successOpList = new ArrayList<>();
    private final List<Op> failureOpList = new ArrayList<>();
    private final Function<TxnRequest, CompletableFuture<TxnResponse>> requestF;

    private boolean seenThen = false;
    private boolean seenElse = false;

    private TxnImpl(Function<TxnRequest, CompletableFuture<TxnResponse>> f, ByteSequence namespace) {
        this.requestF = f;
        this.namespace = namespace;
    }

    @Override
    public TxnImpl If(Cmp... cmps) {
        return If(Arrays.asList(cmps));
    }

    TxnImpl If(List<Cmp> cmps) {
        if (this.seenThen) {
            throw new IllegalArgumentException("cannot call If after Then!");
        }
        if (this.seenElse) {
            throw new IllegalArgumentException("cannot call If after Else!");
        }

        cmpList.addAll(cmps);
        return this;
    }

    @Override
    public TxnImpl Then(Op... ops) {
        return Then(Arrays.asList(ops));
    }

    TxnImpl Then(List<Op> ops) {
        if (this.seenElse) {
            throw new IllegalArgumentException("cannot call Then after Else!");
        }

        this.seenThen = true;

        successOpList.addAll(ops);
        return this;
    }

    @Override
    public TxnImpl Else(Op... ops) {
        return Else(Arrays.asList(ops));
    }

    TxnImpl Else(List<Op> ops) {
        this.seenElse = true;

        failureOpList.addAll(ops);
        return this;
    }

    @Override
    public CompletableFuture<TxnResponse> commit() {
        return this.requestF.apply(this.toTxnRequest());
    }

    private TxnRequest toTxnRequest() {
        TxnRequest.Builder requestBuilder = TxnRequest.newBuilder();

        for (Cmp c : this.cmpList) {
            requestBuilder.addCompare(c.toCompare(namespace));
        }

        for (Op o : this.successOpList) {
            requestBuilder.addSuccess(o.toRequestOp(namespace));
        }

        for (Op o : this.failureOpList) {
            requestBuilder.addFailure(o.toRequestOp(namespace));
        }

        return requestBuilder.build();
    }
}
