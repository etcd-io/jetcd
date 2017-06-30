package com.coreos.jetcd.auth;

import com.coreos.jetcd.data.ByteSequence;

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
