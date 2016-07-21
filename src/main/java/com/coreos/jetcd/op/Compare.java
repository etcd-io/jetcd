package com.coreos.jetcd.op;

import com.google.protobuf.ByteString;

/**
 */
public class Compare {

    public enum Op {
        EQUAL,
        GREATER,
        LESS
    }

    public Compare(ByteString key, Op compareResult, CompareTarget<?> target) {

    }

}
