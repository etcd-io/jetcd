package com.coreos.jetcd.op;

import com.coreos.jetcd.options.PutOption;
import com.google.protobuf.ByteString;
import org.testng.annotations.Test;

public class TxnTest {

  final Cmp CMP = new Cmp(ByteString.copyFromUtf8("key"), Cmp.Op.GREATER,
      CmpTarget.value(ByteString.copyFromUtf8("value")));
  final Op OP = Op
      .put(ByteString.copyFromUtf8("key2"), ByteString.copyFromUtf8("value2"), PutOption.DEFAULT);

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
