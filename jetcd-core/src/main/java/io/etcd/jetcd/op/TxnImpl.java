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

package io.etcd.jetcd.op;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.api.TxnRequest;
import io.etcd.jetcd.kv.TxnResponse;

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

    private List<Cmp> cmpList = new ArrayList<>();
    private List<Op> successOpList = new ArrayList<>();
    private List<Op> failureOpList = new ArrayList<>();
    private Function<TxnRequest, CompletableFuture<TxnResponse>> requestF;

    private boolean seenThen = false;
    private boolean seenElse = false;

    private TxnImpl(Function<TxnRequest, CompletableFuture<TxnResponse>> f, ByteSequence namespace) {
        this.requestF = f;
        this.namespace = namespace;
    }

  //CHECKSTYLE:OFF
  public TxnImpl If(Cmp... cmps) {
    //CHECKSTYLE:ON
        return If(ImmutableList.copyOf(cmps));
    }

  //CHECKSTYLE:OFF
  TxnImpl If(List<Cmp> cmps) {
    //CHECKSTYLE:ON
        if (this.seenThen) {
            throw new IllegalArgumentException("cannot call If after Then!");
        }
        if (this.seenElse) {
            throw new IllegalArgumentException("cannot call If after Else!");
        }

        cmpList.addAll(cmps);
        return this;
    }

  //CHECKSTYLE:OFF
  public TxnImpl Then(Op... ops) {
    //CHECKSTYLE:ON
        return Then(ImmutableList.copyOf(ops));
    }

  //CHECKSTYLE:OFF
  TxnImpl Then(List<Op> ops) {
    //CHECKSTYLE:ON
        if (this.seenElse) {
            throw new IllegalArgumentException("cannot call Then after Else!");
        }

        this.seenThen = true;

        successOpList.addAll(ops);
        return this;
    }

  //CHECKSTYLE:OFF
  public TxnImpl Else(Op... ops) {
    //CHECKSTYLE:ON
        return Else(ImmutableList.copyOf(ops));
    }

  //CHECKSTYLE:OFF
  TxnImpl Else(List<Op> ops) {
    //CHECKSTYLE:ON
        this.seenElse = true;

        failureOpList.addAll(ops);
        return this;
    }

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
