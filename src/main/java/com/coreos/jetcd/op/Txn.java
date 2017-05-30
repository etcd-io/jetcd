package com.coreos.jetcd.op;

import com.coreos.jetcd.api.TxnRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;

/**
 * Build an etcd transaction.
 */
public class Txn {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private List<Cmp> cmpList = Collections.emptyList();
    private List<Op> successOpList = Collections.emptyList();
    private List<Op> failureOpList = Collections.emptyList();

    private Builder() {
    }

    //CHECKSTYLE:OFF
    public Builder If(Cmp... cmps) {
      //CHECKSTYLE:ON
      return If(ImmutableList.copyOf(cmps));
    }

    //CHECKSTYLE:OFF
    public Builder If(List<Cmp> cmps) {
      //CHECKSTYLE:ON
      cmpList.addAll(cmps);
      return this;
    }

    //CHECKSTYLE:OFF
    public Builder Then(Op... ops) {
      //CHECKSTYLE:ON
      return Then(ImmutableList.copyOf(ops));
    }

    //CHECKSTYLE:OFF
    public Builder Then(List<Op> ops) {
      //CHECKSTYLE:ON
      successOpList.addAll(ops);
      return this;
    }

    //CHECKSTYLE:OFF
    public Builder Else(Op... ops) {
      //CHECKSTYLE:ON
      return Else(ImmutableList.copyOf(ops));
    }

    //CHECKSTYLE:OFF
    public Builder Else(List<Op> ops) {
      //CHECKSTYLE:ON
      failureOpList.addAll(ops);
      return this;
    }

    public Txn build() {
      return new Txn(cmpList, successOpList, failureOpList);
    }
  }

  private final List<Cmp> cmpList;
  private final List<Op> successOpList;
  private final List<Op> failureOpList;

  public TxnRequest toTxnRequest() {
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

  private Txn(List<Cmp> cmpList, List<Op> successOpList, List<Op> failureOpList) {
    this.cmpList = cmpList;
    this.successOpList = successOpList;
    this.failureOpList = failureOpList;
  }
}
