package com.coreos.jetcd.op;

import com.coreos.jetcd.api.TxnRequest;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * Build an etcd transaction.
 */
public class Txn {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private List<Cmp> cmpList = new ArrayList<>();
    private List<Op> successOpList = new ArrayList<>();
    private List<Op> failureOpList = new ArrayList<>();

    private boolean seenThen = false;
    private boolean seenElse = false;

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
    public Builder Then(Op... ops) {
      //CHECKSTYLE:ON
      return Then(ImmutableList.copyOf(ops));
    }

    //CHECKSTYLE:OFF
    public Builder Then(List<Op> ops) {
      //CHECKSTYLE:ON
      if (this.seenElse) {
        throw new IllegalArgumentException("cannot call Then after Else!");
      }

      this.seenThen = true;

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
      this.seenElse = true;

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
