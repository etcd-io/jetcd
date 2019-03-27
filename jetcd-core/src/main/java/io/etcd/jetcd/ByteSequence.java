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

package io.etcd.jetcd;

import com.google.protobuf.ByteString;
import java.nio.charset.Charset;

/**
 * Etcd binary bytes, easy to convert between byte[], String and ByteString.
 */
public final class ByteSequence {
  public static final ByteSequence EMPTY = new ByteSequence(ByteString.EMPTY);
  public static final ByteSequence NAMESPACE_DELIMITER = ByteSequence.from(new byte[] {'/'});

  private final int hashVal;
  private final ByteString byteString;

  private ByteSequence(ByteString byteString) {
    this.byteString = byteString;
    this.hashVal = byteString.hashCode();
  }

  public boolean startsWith(ByteSequence prefix) {
    ByteString baseByteString = this.getByteString();
    ByteString prefixByteString = prefix.getByteString();
    return baseByteString.startsWith(prefixByteString);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ByteSequence) {
      ByteSequence other = (ByteSequence) obj;
      if (other.hashCode() != hashCode()) {
        return false;
      }
      return byteString.equals(other.byteString);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return hashVal;
  }

  ByteString getByteString() {
    return this.byteString;
  }

  public String toString(Charset charset) {
    return byteString.toString(charset);
  }

  public byte[] getBytes() {
    return byteString.toByteArray();
  }

  public boolean isEmpty() {
    return byteString.isEmpty();
  }

  public int size() {
    return byteString.size();
  }

  /**
   * Create new ByteSequence from a String.
   * @param source input String
   * @param charset the character set to use to transform the String into bytes
   * @return the ByteSequence
   */
  public static ByteSequence from(String source, Charset charset) {
    byte[] bytes = source.getBytes(charset);

    return new ByteSequence(ByteString.copyFrom(bytes));
  }

  public static ByteSequence from(ByteString source) {
    return new ByteSequence(source);
  }

  public static ByteSequence from(byte[] source) {
    return new ByteSequence(ByteString.copyFrom(source));
  }

}
