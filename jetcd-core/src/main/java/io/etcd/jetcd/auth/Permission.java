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

package io.etcd.jetcd.auth;

import io.etcd.jetcd.ByteSequence;

/**
 * represents a permission over a range of keys.
 */
public class Permission {

  private final Type permType;
  private final ByteSequence key;
  private final ByteSequence rangeEnd;

  public enum Type {
    READ,
    WRITE,
    READWRITE,
    UNRECOGNIZED,
  }

  public Permission(Type permType, ByteSequence key, ByteSequence rangeEnd) {
    this.permType = permType;
    this.key = key;
    this.rangeEnd = rangeEnd;
  }

  /**
   * returns the type of Permission: READ, WRITE, READWRITE, or UNRECOGNIZED.
   */
  public Type getPermType() {
    return permType;
  }

  public ByteSequence getKey() {
    return key;
  }

  public ByteSequence getRangeEnd() {
    return rangeEnd;
  }
}
