package com.coreos.jetcd.options;

import com.google.common.base.Optional;
import com.google.protobuf.ByteString;

public final class DeleteOption {

  public static final DeleteOption DEFAULT = newBuilder().build();

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Optional<ByteString> endKey = Optional.absent();
    private boolean prevKV = false;

    private Builder() {
    }

    /**
     * Set the end key of the delete request. If it is set, the delete request will delete the keys
     * from <i>key</i> to <i>endKey</i> (exclusive).
     *
     * <p>If end key is '\0', the range is all keys >=
     * key.
     *
     * <p>If the end key is one bit larger than the given key, then it deletes all keys with
     * the prefix (the given key).
     *
     * <p>If both key and end key are '\0', it deletes all keys.
     *
     * @param endKey end key
     * @return builder
     */
    public Builder withRange(ByteString endKey) {
      this.endKey = Optional.fromNullable(endKey);
      return this;
    }

    /**
     * Get the previous key/value pairs before deleting them.
     *
     * @param prevKV flag to get previous key/value pairs before deleting them.
     * @return builder
     */
    public Builder withPrevKV(boolean prevKV) {
      this.prevKV = prevKV;
      return this;
    }

    public DeleteOption build() {
      return new DeleteOption(endKey, prevKV);
    }

  }

  private final Optional<ByteString> endKey;
  private final boolean prevKV;

  private DeleteOption(Optional<ByteString> endKey, boolean prevKV) {
    this.endKey = endKey;
    this.prevKV = prevKV;
  }

  public Optional<ByteString> getEndKey() {
    return endKey;
  }

  /**
   * Whether to get the previous key/value pairs before deleting them.
   *
   * @return true if get the previous key/value pairs before deleting them, otherwise false.
   */
  public boolean isPrevKV() {
    return prevKV;
  }
}
