package com.coreos.jetcd.op;

import com.coreos.jetcd.api.TxnRequest;
import com.coreos.jetcd.kv.TxnResponse;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Build an etcd transaction.
 */
public class TxnImpl implements com.coreos.jetcd.Txn {

  public static TxnImpl newTxn(Function<TxnRequest, CompletableFuture<TxnResponse>> f) {
    return new TxnImpl(f);
  }

  private List<Cmp> cmpList = new ArrayList<>();
  private List<Op> successOpList = new ArrayList<>();
  private List<Op> failureOpList = new ArrayList<>();
  private Function<TxnRequest, CompletableFuture<TxnResponse>> requestF;

  private boolean seenThen = false;
  private boolean seenElse = false;

  private TxnImpl(Function<TxnRequest, CompletableFuture<TxnResponse>> f) {
    this.requestF = f;
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
      requestBuilder.addCompare(c.toCompare());
    }

    for (Op o : this.successOpList) {
      requestBuilder.addSuccess(o.toRequestOp());
    }

    for (Op o : this.failureOpList) {
      requestBuilder.addFailure(o.toRequestOp());
    }

    return requestBuilder.build();
  }
}
