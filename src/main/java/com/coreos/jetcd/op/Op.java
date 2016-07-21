package com.coreos.jetcd.op;

import com.coreos.jetcd.options.PutOption;
import com.google.protobuf.ByteString;

/**
 * Etcd Operation
 */
public abstract class Op {

    public static PutOp put(ByteString key, ByteString value, PutOption option) {
        return new PutOp(key, value, option);
    }

    /**
     * Operation type.
     */
    public enum Type {
        PUT,
        RANGE,
        DELETE_RANGE,
    }

    protected final ByteString key;

    protected Op(Type type, ByteString key) {
        this.key = key;
    }

    public static final class PutOp extends Op {

        private final PutOption option;

        protected PutOp(ByteString key, ByteString value, PutOption option) {
            super(Type.PUT, key);
            this.option = option;
        }
    }

}
