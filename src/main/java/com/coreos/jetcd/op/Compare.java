package com.coreos.jetcd.op;

import com.google.protobuf.ByteString;

/**
 * The compare predicate in {@link Txn}
 */
public class Compare {

    public enum Op {
        EQUAL,
        GREATER,
        LESS
    }

    private final ByteString key;
    private final Op op;
    private final CompareTarget<?> target;

    public Compare(ByteString key, Op compareOp, CompareTarget<?> target) {
        this.key = key;
        this.op = compareOp;
        this.target = target;
    }

    /**
     * Get the key to compare.
     *
     * @return the key to compare
     */
    public ByteString getKey() {
        return key;
    }

    /**
     * Get the compare op.
     *
     * @return the compare op.
     */
    public Op getOp() {
        return op;
    }

    /**
     * Get the target to compare.
     *
     * @return the target to compare.
     */
    public CompareTarget<?> getTarget() {
        return target;
    }

}
