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

package io.etcd.jetcd;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SecureByteSequenceTest {

    @Test
    public void testFromString() {
        String testString = "sensitive data";
        try (SecureByteSequence sbs = SecureByteSequence.from(testString)) {
            assertThat(sbs.getBytes()).isEqualTo(testString.getBytes(StandardCharsets.UTF_8));
            assertThat(sbs.size()).isEqualTo(testString.length());
            assertThat(sbs.isEmpty()).isFalse();
            assertThat(sbs.isClosed()).isFalse();
        }
    }

    @Test
    public void testFromByteArray() {
        byte[] testBytes = new byte[] { 1, 2, 3, 4, 5 };
        try (SecureByteSequence sbs = SecureByteSequence.from(testBytes)) {
            assertThat(sbs.getBytes()).isEqualTo(testBytes);
            assertThat(sbs.size()).isEqualTo(5);

            // Verify defensive copy - modifying original should not affect SecureByteSequence
            testBytes[0] = 99;
            assertThat(sbs.getBytes()[0]).isEqualTo((byte) 1);
        }
    }

    @Test
    public void testFromCharArray() {
        char[] testChars = "password123".toCharArray();
        try (SecureByteSequence sbs = SecureByteSequence.from(testChars)) {
            byte[] expected = "password123".getBytes(StandardCharsets.UTF_8);
            assertThat(sbs.getBytes()).isEqualTo(expected);
            assertThat(sbs.size()).isEqualTo(expected.length);
        }
    }

    @Test
    public void testToByteSequence() {
        String testString = "test";
        try (SecureByteSequence sbs = SecureByteSequence.from(testString)) {
            ByteSequence bs = sbs.toByteSequence();
            assertThat(bs.getBytes()).isEqualTo(testString.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testClose() {
        SecureByteSequence sbs = SecureByteSequence.from("sensitive");
        assertThat(sbs.isClosed()).isFalse();

        sbs.close();
        assertThat(sbs.isClosed()).isTrue();
    }

    @Test
    public void testAccessAfterClose() {
        SecureByteSequence sbs = SecureByteSequence.from("test");
        sbs.close();

        assertThatThrownBy(sbs::getBytes)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("closed");

        assertThatThrownBy(sbs::toByteSequence)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("closed");
    }

    @Test
    public void testIdempotentClose() {
        SecureByteSequence sbs = SecureByteSequence.from("test");
        sbs.close();
        sbs.close(); // Should not throw
        sbs.close(); // Should not throw

        assertThat(sbs.isClosed()).isTrue();
    }

    @Test
    public void testTryWithResources() {
        byte[] testBytes = "password".getBytes(StandardCharsets.UTF_8);
        SecureByteSequence sbs;

        try (SecureByteSequence temp = SecureByteSequence.from(testBytes)) {
            sbs = temp;
            assertThat(sbs.isClosed()).isFalse();
            assertThat(sbs.getBytes()).isEqualTo(testBytes);
        }

        // After try-with-resources, should be closed
        assertThat(sbs.isClosed()).isTrue();
    }

    @Test
    public void testToString() {
        try (SecureByteSequence sbs = SecureByteSequence.from("secret")) {
            String str = sbs.toString();
            // Should not expose actual content
            assertThat(str).doesNotContain("secret");
            assertThat(str).contains("SECURE");
            assertThat(str).contains("6 bytes");
        }
    }

    @Test
    public void testToStringAfterClose() {
        SecureByteSequence sbs = SecureByteSequence.from("test");
        sbs.close();

        assertThat(sbs.toString()).isEqualTo("[CLOSED]");
    }

    @Test
    public void testToStringWithCharset() {
        try (SecureByteSequence sbs = SecureByteSequence.from("test")) {
            String str = sbs.toString(StandardCharsets.UTF_8);
            assertThat(str).isEqualTo("test");
        }
    }

    @Test
    public void testToStringWithCharsetAfterClose() {
        SecureByteSequence sbs = SecureByteSequence.from("test");
        sbs.close();

        assertThat(sbs.toString(StandardCharsets.UTF_8)).isEqualTo("[CLOSED]");
    }

    @Test
    public void testIsEmpty() {
        try (SecureByteSequence empty = SecureByteSequence.from("");
            SecureByteSequence notEmpty = SecureByteSequence.from("data")) {

            assertThat(empty.isEmpty()).isTrue();
            assertThat(notEmpty.isEmpty()).isFalse();
        }
    }

    @Test
    public void testSize() {
        try (SecureByteSequence sbs = SecureByteSequence.from("12345")) {
            assertThat(sbs.size()).isEqualTo(5);
        }
    }

    @Test
    public void testDataIsZeroed() throws Exception {
        byte[] originalData = "sensitive".getBytes(StandardCharsets.UTF_8);
        SecureByteSequence sbs = SecureByteSequence.from(originalData);

        // Get a reference to internal bytes via reflection (for testing only)
        java.lang.reflect.Field bytesField = SecureByteSequence.class.getDeclaredField("bytes");
        bytesField.setAccessible(true);
        byte[] internalBytes = (byte[]) bytesField.get(sbs);

        // Verify data exists before close
        assertThat(internalBytes).isNotEqualTo(new byte[internalBytes.length]);

        // Close and verify data is zeroed
        sbs.close();
        assertThat(internalBytes).isEqualTo(new byte[internalBytes.length]);
    }
}
