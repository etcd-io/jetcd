/*
 * Copyright 2016-2020 The jetcd authors
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

import com.google.protobuf.ByteString;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Util;
import io.etcd.jetcd.api.Compare;

/**
 * The compare predicate in {@link io.etcd.jetcd.Txn}.
 */
public class Cmp {

    public enum Op {
        EQUAL, GREATER, LESS
    }

    private final ByteString key;
    private final Op op;
    private final CmpTarget<?> target;

    public Cmp(ByteSequence key, Op compareOp, CmpTarget<?> target) {
        this.key = ByteString.copyFrom(key.getBytes());
        this.op = compareOp;
        this.target = target;
    }

    Compare toCompare(ByteSequence namespace) {
        Compare.Builder compareBuilder = Compare.newBuilder().setKey(Util.prefixNamespace(this.key, namespace));
        switch (this.op) {
            case EQUAL:
                compareBuilder.setResult(Compare.CompareResult.EQUAL);
                break;
            case GREATER:
                compareBuilder.setResult(Compare.CompareResult.GREATER);
                break;
            case LESS:
                compareBuilder.setResult(Compare.CompareResult.LESS);
                break;
            default:
                throw new IllegalArgumentException("Unexpected compare type (" + this.op + ")");
        }

        Compare.CompareTarget target = this.target.getTarget();
        Object value = this.target.getTargetValue();

        compareBuilder.setTarget(target);
        switch (target) {
            case VERSION:
                compareBuilder.setVersion((Long) value);
                break;
            case VALUE:
                compareBuilder.setValue((ByteString) value);
                break;
            case MOD:
                compareBuilder.setModRevision((Long) value);
                break;
            case CREATE:
                compareBuilder.setCreateRevision((Long) value);
                break;
            default:
                throw new IllegalArgumentException("Unexpected target type (" + target + ")");
        }

        return compareBuilder.build();
    }
}
