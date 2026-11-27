/*
 * Copyright 2016-2021 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd.op;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.api.Compare;
import io.etcd.jetcd.support.Util;

import com.google.protobuf.ByteString;

/**
 * The compare predicate in {@link Txn}.
 */
public class Cmp {

    /**
     * Comparison operations.
     */
    public enum Op {
        /** Equal comparison. */
        EQUAL,
        /** Greater than comparison. */
        GREATER,
        /** Less than comparison. */
        LESS,
        /** Not equal comparison. */
        NOT_EQUAL
    }

    private final ByteString key;
    private final Op op;
    private final CmpTarget<?> target;

    /**
     * Creates a new comparison.
     *
     * @param key       the key to compare
     * @param compareOp the comparison operation
     * @param target    the comparison target
     */
    public Cmp(ByteSequence key, Op compareOp, CmpTarget<?> target) {
        this.key = ByteString.copyFrom(key.getBytes());
        this.op = compareOp;
        this.target = target;
    }

    Compare toCompare(ByteSequence namespace) {
        Compare.Builder compareBuilder = Compare.newBuilder().setKey(Util.prefixNamespace(this.key, namespace));
        Compare.CompareResult result = switch (this.op) {
            case EQUAL -> Compare.CompareResult.EQUAL;
            case GREATER -> Compare.CompareResult.GREATER;
            case LESS -> Compare.CompareResult.LESS;
            case NOT_EQUAL -> Compare.CompareResult.NOT_EQUAL;
            default -> throw new IllegalArgumentException("Unexpected compare type (" + this.op + ")");
        };
        compareBuilder.setResult(result);

        Compare.CompareTarget target = this.target.getTarget();
        Object value = this.target.getTargetValue();

        compareBuilder.setTarget(target);
        switch (target) {
            case VERSION -> compareBuilder.setVersion((Long) value);
            case VALUE -> compareBuilder.setValue((ByteString) value);
            case MOD -> compareBuilder.setModRevision((Long) value);
            case CREATE -> compareBuilder.setCreateRevision((Long) value);
            default -> throw new IllegalArgumentException("Unexpected target type (" + target + ")");
        }

        return compareBuilder.build();
    }
}
