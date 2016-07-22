package com.coreos.jetcd.op;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Comparator;
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

        private List<Compare> compareList = (List<Compare>) EMPTY_LIST;
        private List<Op> successOpList = (List<Op>) EMPTY_LIST;
        private List<Op> failureOpList = (List<Op>) EMPTY_LIST;

        private Builder() {}

        public Builder If(Compare... compares) {
            compareList = Lists.newArrayList(compares);
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
            return new Txn(compareList, successOpList, failureOpList);
        }

    }

    private final List<Compare> compareList;
    private final List<Op> successOpList;
    private final List<Op> failureOpList;

    private Txn(List<Compare> compareList,
                List<Op> successOpList,
                List<Op> failureOpList) {
        this.compareList = compareList;
        this.successOpList = successOpList;
        this.failureOpList = failureOpList;
    }

}
