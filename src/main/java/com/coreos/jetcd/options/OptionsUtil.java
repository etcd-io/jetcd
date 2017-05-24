package com.coreos.jetcd.options;

import com.google.protobuf.ByteString;
import java.util.Arrays;

public class OptionsUtil {

  private static final byte[] NO_PREFIX_END = {0};

  private OptionsUtil() {
  }

  /**
   * Gets the range end of the given prefix.
   *
   * <p>The range end is the key plus one (e.g., "aa"+1 == "ab", "a\xff"+1 == "b").
   *
   * @param prefix the given prefix
   * @return the range end of the given prefix
   */
  static final ByteString prefixEndOf(ByteString prefix) {
    byte[] endKey = prefix.toByteArray().clone();
    for (int i = endKey.length - 1; i >= 0; i--) {
      if (endKey[i] < 0xff) {
        endKey[i] = (byte) (endKey[i] + 1);
        return ByteString.copyFrom(Arrays.copyOf(endKey, i + 1));
      }
    }

    return ByteString.copyFrom(NO_PREFIX_END);
  }
}
