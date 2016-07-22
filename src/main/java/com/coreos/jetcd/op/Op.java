package com.coreos.jetcd.op;

import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.protobuf.ByteString;

/**
 * Etcd Operation
 */
public abstract class Op {

    public static PutOp put(ByteString key, ByteString value, PutOption option) {
        return new PutOp(key, value, option);
    }

    public static GetOp get(ByteString key, GetOption option) {
        return new GetOp(key, option);
    }

    public static DeleteOp delete(ByteString key, DeleteOption option) {
        return new DeleteOp(key, option);
    }

    /**
     * Operation type.
     */
    public enum Type {
        PUT,
        RANGE,
        DELETE_RANGE,
    }

    protected final Type type;
    protected final ByteString key;

    protected Op(Type type, ByteString key) {
        this.type = type;
        this.key = key;
    }

    public Type getType() {
        return this.type;
    }

    public ByteString getKey() {
        return this.key;
    }

    public static final class PutOp extends Op {

        private final ByteString value;
        private final PutOption option;

        protected PutOp(ByteString key, ByteString value, PutOption option) {
            super(Type.PUT, key);
            this.value = value;
            this.option = option;
        }

        public ByteString getValue() {
            return value;
        }

        public PutOption getOption() {
            return option;
        }
    }

    public static final class GetOp extends Op {

        private final GetOption option;

        protected GetOp(ByteString key, GetOption option) {
            super(Type.RANGE, key);
            this.option = option;
        }

        public GetOption getOption() {
            return this.option;
        }
    }

    public static final class DeleteOp extends Op {

        private final DeleteOption option;

        protected DeleteOp(ByteString key, DeleteOption option) {
            super(Type.DELETE_RANGE, key);
            this.option = option;
        }

        public DeleteOption getOption() {
            return this.option;
        }
    }

}
