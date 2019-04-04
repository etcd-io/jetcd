/*
 * Copyright 2016-2019 The jetcd authors
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

import static org.assertj.core.api.Assertions.assertThat;

import io.etcd.jetcd.ByteSequence;
import org.junit.jupiter.api.Test;

public class OptionsUtilTest {

  static void check(byte[] prefix, byte[] expectedPrefixEndOf) {
    ByteSequence actual = OptionsUtil.prefixEndOf(ByteSequence.from(prefix));
    assertThat(actual).isEqualTo(ByteSequence.from(expectedPrefixEndOf));
  }

  @Test
  void aaPlus1() {
    check(new byte[]{(byte) 'a', (byte) 'a'}, new byte[]{(byte) 'a', (byte) 'b'});
  }

  @Test
  void axffPlus1() {
    check(new byte[]{(byte) 'a', (byte) 0xff}, new byte[]{(byte) 'b'});
  }

  @Test
  void xffPlus1() {
    check(new byte[]{(byte) 0xff}, new byte[]{(byte) 0x00});
  }
}

