package com.coreos.jetcd.util;

import com.google.protobuf.ByteString;

import java.util.Arrays;

public abstract class KeyPrefixUtil {

  private static final byte[] NO_PREFIX_END = {0};

  /**
   * Gets the range end of the given prefix.
   *
   * <p>The range end is the key plus one (e.g., "aa"+1 == "ab", "a\xff"+1 == "b").
   *
   * @param prefix the given prefix
   * @return the range end of the given prefix
   */
  public static final ByteString prefixEndOf(ByteString prefix) {
    byte[] keyBytes = prefix.toByteArray();
    boolean hasPrefix = false;
    int i = keyBytes.length - 1;
    for (; i >= 0; i--) {
      if (keyBytes[i] < 0xff) {
        hasPrefix = true;
        break;
      }
    }

    if (hasPrefix) {
      byte[] endKey = Arrays.copyOfRange(keyBytes, 0, i + 1);
      endKey[i] = (byte) (keyBytes[i] + 1);
      return ByteString.copyFrom(endKey);
    }

    return ByteString.copyFrom(NO_PREFIX_END);
  }

}
