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

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A secure byte sequence that zeroes its content when closed.
 *
 * <p>
 * This class is designed for storing sensitive data such as passwords or tokens.
 * The backing byte array is zeroed when {@link #close()} is called to prevent
 * sensitive data from lingering in memory.
 *
 * <p>
 * <b>Usage:</b>
 *
 * <pre>{@code
 * try (SecureByteSequence password = SecureByteSequence.from("myPassword")) {
 *     client.user(username).password(password).build();
 * } // password is automatically zeroed
 * }</pre>
 *
 * <p>
 * <b>Important:</b>
 * <ul>
 * <li>Always use try-with-resources to ensure automatic cleanup
 * <li>Do not share instances across threads without synchronization
 * <li>Do not call {@link #getBytes()} and store the result (defeats the purpose)
 * <li>This provides defense-in-depth but is not foolproof against determined attackers
 * </ul>
 *
 * @see ByteSequence
 */
public final class SecureByteSequence implements AutoCloseable {
    private final byte[] bytes;
    private final AtomicBoolean closed;

    private SecureByteSequence(byte[] bytes) {
        this.bytes = bytes;
        this.closed = new AtomicBoolean(false);
    }

    /**
     * Creates a SecureByteSequence from a string.
     * The string is converted to UTF-8 bytes.
     *
     * @param  str the string to convert
     * @return     a new SecureByteSequence
     */
    public static SecureByteSequence from(String str) {
        return new SecureByteSequence(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a SecureByteSequence from a byte array.
     * The array is copied to prevent external modification.
     *
     * @param  bytes the byte array
     * @return       a new SecureByteSequence
     */
    public static SecureByteSequence from(byte[] bytes) {
        return new SecureByteSequence(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Creates a SecureByteSequence from a char array.
     * Useful when reading passwords from Console.readPassword().
     *
     * @param  chars the char array
     * @return       a new SecureByteSequence
     */
    public static SecureByteSequence from(char[] chars) {
        java.nio.ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new SecureByteSequence(bytes);
    }

    /**
     * Converts this SecureByteSequence to a ByteSequence.
     * Note: The returned ByteSequence will not be zeroed when this SecureByteSequence is closed.
     *
     * @return a ByteSequence containing the same data
     */
    public ByteSequence toByteSequence() {
        if (closed.get()) {
            throw new IllegalStateException("SecureByteSequence has been closed");
        }
        return ByteSequence.from(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Returns the bytes of this SecureByteSequence.
     * Warning: The returned array is a copy but should be zeroed after use.
     *
     * @return                       a copy of the byte array
     * @throws IllegalStateException if this SecureByteSequence has been closed
     */
    public byte[] getBytes() {
        if (closed.get()) {
            throw new IllegalStateException("SecureByteSequence has been closed");
        }
        return Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Returns the size of this SecureByteSequence.
     *
     * @return the number of bytes
     */
    public int size() {
        return bytes.length;
    }

    /**
     * Returns true if this SecureByteSequence is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public String toString() {
        if (closed.get()) {
            return "[CLOSED]";
        }
        return "[SECURE:" + bytes.length + " bytes]";
    }

    /**
     * Returns the string representation using the specified charset.
     * Warning: This exposes the sensitive data. Use with caution.
     *
     * @param  charset the charset to use
     * @return         the string representation
     */
    public String toString(Charset charset) {
        if (closed.get()) {
            return "[CLOSED]";
        }
        return new String(bytes, charset);
    }

    /**
     * Zeroes the backing byte array and marks this sequence as closed.
     * After calling close(), any attempt to access the data will throw IllegalStateException.
     *
     * <p>
     * This method is idempotent - calling it multiple times is safe.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * Checks if this SecureByteSequence has been closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }
}
