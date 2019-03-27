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

import static com.google.common.base.Preconditions.checkNotNull;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import java.util.Optional;


public final class DeleteOption {

  public static final DeleteOption DEFAULT = newBuilder().build();

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Optional<ByteSequence> endKey = Optional.empty();
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
    public Builder withRange(ByteSequence endKey) {
      this.endKey = Optional.ofNullable(endKey);
      return this;
    }

    /**
     * Enables 'Delete' requests to delete all the keys with matching prefix.
     *
     * <p>You should pass the key that is passed into
     * {@link KV#delete(ByteSequence) KV.delete} method
     * into this method as the given key.
     *
     * @param prefix the common prefix of all the keys that you want to delete
     * @return builder
     */
    public Builder withPrefix(ByteSequence prefix) {
      checkNotNull(prefix, "prefix should not be null");
      ByteSequence prefixEnd = OptionsUtil.prefixEndOf(prefix);
      this.withRange(prefixEnd);
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

  private final Optional<ByteSequence> endKey;
  private final boolean prevKV;

  private DeleteOption(Optional<ByteSequence> endKey, boolean prevKV) {
    this.endKey = endKey;
    this.prevKV = prevKV;
  }

  public Optional<ByteSequence> getEndKey() {
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
