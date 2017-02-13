package com.coreos.jetcd.op;

import com.coreos.jetcd.api.TxnRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Build an etcd transaction.
 */
public class Txn {

  private final static ImmutableList<?> EMPTY_LIST = ImmutableList.copyOf(new Object[0]);

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private List<Cmp> cmpList = (List<Cmp>) EMPTY_LIST;
    private List<Op> successOpList = (List<Op>) EMPTY_LIST;
    private List<Op> failureOpList = (List<Op>) EMPTY_LIST;

    private Builder() {
    }

    public Builder If(Cmp... cmps) {
      cmpList = Lists.newArrayList(cmps);
      return this;
    }

    public Builder Then(Op... ops) {
      successOpList = Lists.newArrayList(ops);
      return this;
    }

    public Builder Else(Op... ops) {
      failureOpList = Lists.newArrayList(ops);
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
