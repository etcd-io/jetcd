package com.coreos.jetcd.op;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.PutOption;
import org.testng.annotations.Test;

public class TxnTest {

  final Cmp CMP = new Cmp(ByteSequence.fromString("key"), Cmp.Op.GREATER,
      CmpTarget.value(ByteSequence.fromString("value")));
  final Op OP = Op
      .put(ByteSequence.fromString("key2"), ByteSequence.fromString("value2"), PutOption.DEFAULT);

  @Test
  public void testIfs() {
    Txn.newBuilder().If(CMP).If(CMP).build();
  }

  @Test
  public void testThens() {
    Txn.newBuilder().Then(OP).Then(OP).build();
  }

  @Test
  public void testElses() {
    Txn.newBuilder().Else(OP).Else(OP).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "cannot call If after Then!")
  public void testIfAfterThen() {
    Txn.newBuilder().Then(OP).If(CMP).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "cannot call If after Else!")
  public void testIfAfterElse() {
    Txn.newBuilder().Else(OP).If(CMP).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "cannot call Then after Else!")
  public void testThenAfterElse() {
    Txn.newBuilder().Else(OP).Then(OP).build();
  }
}
