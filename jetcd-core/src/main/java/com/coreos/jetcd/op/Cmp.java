/**
 * Copyright 2017 The jetcd authors
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

package com.coreos.jetcd.op;

import com.coreos.jetcd.api.Compare;
import com.coreos.jetcd.data.ByteSequence;
import com.google.protobuf.ByteString;

/**
 * The compare predicate in {@link Txn}.
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

  Compare toCompare() {
    Compare.Builder compareBuiler = Compare.newBuilder().setKey(this.key);
    switch (this.op) {
      case EQUAL:
        compareBuiler.setResult(Compare.CompareResult.EQUAL);
        break;
      case GREATER:
        compareBuiler.setResult(Compare.CompareResult.GREATER);
        break;
      case LESS:
        compareBuiler.setResult(Compare.CompareResult.LESS);
        break;
      default:
        throw new IllegalArgumentException("Unexpected compare type (" + this.op + ")");
    }

    Compare.CompareTarget target = this.target.getTarget();
    Object value = this.target.getTargetValue();

    compareBuiler.setTarget(target);
    switch (target) {
      case VERSION:
        compareBuiler.setVersion((Long) value);
        break;
      case VALUE:
        compareBuiler.setValue((ByteString) value);
        break;
      case MOD:
        compareBuiler.setModRevision((Long) value);
        break;
      case CREATE:
        compareBuiler.setCreateRevision((Long) value);
        break;
      default:
        throw new IllegalArgumentException("Unexpected target type (" + target + ")");
    }

    return compareBuiler.build();
  }
}
