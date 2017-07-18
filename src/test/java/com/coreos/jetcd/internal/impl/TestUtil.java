package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.data.ByteSequence;

public class TestUtil {

  public static String randomString() {
    return java.util.UUID.randomUUID().toString();
  }

  public static ByteSequence randomByteSequence() {
    return ByteSequence.fromString(randomString());
  }
}
