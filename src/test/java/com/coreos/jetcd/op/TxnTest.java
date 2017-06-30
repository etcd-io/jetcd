package com.coreos.jetcd.op;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.PutOption;
import org.junit.Test;

public class TxnTest {

  final Cmp CMP = new Cmp(ByteSequence.fromString("key"), Cmp.Op.GREATER,
      CmpTarget.value(ByteSequence.fromString("value")));
  final Op OP = Op
      .put(ByteSequence.fromString("key2"), ByteSequence.fromString("value2"), PutOption.DEFAULT);

  @Test
  public void testIfs() {
    TxnImpl.newTxn((t) -> null).If(CMP).If(CMP).commit();
  }

  @Test
  public void testThens() {
    TxnImpl.newTxn((t) -> null).Then(OP).Then(OP).commit();
  }

  @Test
  public void testElses() {
    TxnImpl.newTxn((t) -> null).Else(OP).Else(OP).commit();
  }

  @Test
  public void testIfAfterThen() {
    assertThatThrownBy(() -> TxnImpl.newTxn((t) -> null).Then(OP).If(CMP).commit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot call If after Then!");
  }

  @Test
  public void testIfAfterElse() {
    assertThatThrownBy(() -> TxnImpl.newTxn((t) -> null).Else(OP).If(CMP).commit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot call If after Else!");
  }

  @Test
  public void testThenAfterElse() {
    assertThatThrownBy(() -> TxnImpl.newTxn((t) -> null).Else(OP).Then(OP).commit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot call Then after Else!");
  }
}
