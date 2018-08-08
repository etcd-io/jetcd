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

package io.etcd.jetcd.options;

import io.etcd.jetcd.api.RangeRequest;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.options.GetOption.SortOrder;
import io.etcd.jetcd.options.GetOption.SortTarget;
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
  static final ByteSequence prefixEndOf(ByteSequence prefix) {
    byte[] endKey = prefix.getBytes().clone();
    for (int i = endKey.length - 1; i >= 0; i--) {
      if (endKey[i] < 0xff) {
        endKey[i] = (byte) (endKey[i] + 1);
        return ByteSequence.fromBytes(Arrays.copyOf(endKey, i + 1));
      }
    }

    return ByteSequence.fromBytes(NO_PREFIX_END);
  }

  /**
   * convert client SortOrder to apu SortOrder.
   */
  public static RangeRequest.SortOrder toRangeRequestSortOrder(SortOrder order) {
    switch (order) {
      case NONE:
        return RangeRequest.SortOrder.NONE;
      case ASCEND:
        return RangeRequest.SortOrder.ASCEND;
      case DESCEND:
        return RangeRequest.SortOrder.DESCEND;
      default:
        return RangeRequest.SortOrder.UNRECOGNIZED;
    }
  }

  /**
   * convert client SortTarget to apu SortTarget.
   */
  public static RangeRequest.SortTarget toRangeRequestSortTarget(SortTarget target) {
    switch (target) {
      case KEY:
        return RangeRequest.SortTarget.KEY;
      case CREATE:
        return RangeRequest.SortTarget.CREATE;
      case MOD:
        return RangeRequest.SortTarget.MOD;
      case VALUE:
        return RangeRequest.SortTarget.VALUE;
      case VERSION:
        return RangeRequest.SortTarget.VERSION;
      default:
        return RangeRequest.SortTarget.UNRECOGNIZED;
    }
  }
}
