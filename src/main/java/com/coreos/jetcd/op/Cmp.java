package com.coreos.jetcd.op;

import com.coreos.jetcd.api.Compare;
import com.google.protobuf.ByteString;

/**
 * The compare predicate in {@link Txn}
 */
public class Cmp {

  public enum Op {
    EQUAL, GREATER, LESS
  }

  private final ByteString key;
  private final Op op;
  private final CmpTarget<?> target;

  public Cmp(ByteString key, Op compareOp, CmpTarget<?> target) {
    this.key = key;
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
      case CREATE:
        compareBuiler.setCreateRevision((Long) value);
      default:
        throw new IllegalArgumentException("Unexpected target type (" + target + ")");
    }

    return compareBuiler.build();
  }
}
