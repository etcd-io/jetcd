package com.coreos.jetcd.op;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Build an etcd transaction.
 */
public class Txn {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private List<Compare> compareList;
        private List<Op> successOpList;
        private List<Op> failureOpList;

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
            // TODO: (sijie) add validations
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
