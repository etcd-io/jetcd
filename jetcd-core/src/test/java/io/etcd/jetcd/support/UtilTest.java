/*
 * Copyright 2016-2021 The jetcd authors
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

package io.etcd.jetcd.support;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.etcd.jetcd.ByteSequence;

import com.google.protobuf.ByteString;

import static org.assertj.core.api.Assertions.assertThat;

class UtilTest {

    static Stream<Arguments> rangeEndProvider() {
        return Stream.of(
            Arguments.of(new byte[] { 'a', (byte) 0xff }, new byte[] { 0 }, new byte[] { 'b' }),
            Arguments.of(new byte[] { 'a', (byte) 0xff, (byte) 0xff }, new byte[] { 0 }, new byte[] { 'b' }),
            Arguments.of(new byte[] { 0x7f, (byte) 0xff }, new byte[] { 0 }, new byte[] { (byte) 0x80 }),
            Arguments.of(new byte[] { 'a' }, new byte[] { 0 }, new byte[] { 'b' }),
            Arguments.of(new byte[] { (byte) 0xff, (byte) 0xff }, new byte[] { 0 }, new byte[] { 0 }),
            Arguments.of(new byte[] {}, new byte[] { 0 }, new byte[] { 0 }),
            Arguments.of(new byte[] { 'a', (byte) 0xff }, new byte[] { 'z' }, new byte[] { 'a', (byte) 0xff, 'z' }),
            Arguments.of(new byte[] { 'a', (byte) 0xff }, new byte[] {}, new byte[] { 'a', (byte) 0xff }));
    }

    @ParameterizedTest
    @MethodSource("rangeEndProvider")
    void prefixByteSequenceRangeEnd(byte[] namespace, byte[] end, byte[] expected) {
        assertThat(Util.prefixNamespaceToRangeEnd(ByteSequence.from(end), ByteSequence.from(namespace)))
            .isEqualTo(ByteString.copyFrom(expected));
    }

    @ParameterizedTest
    @MethodSource("rangeEndProvider")
    void prefixByteStringRangeEnd(byte[] namespace, byte[] end, byte[] expected) {
        assertThat(Util.prefixNamespaceToRangeEnd(ByteString.copyFrom(end), ByteSequence.from(namespace)))
            .isEqualTo(ByteString.copyFrom(expected));
    }
}
