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

package io.etcd.jetcd.options;

import java.util.Optional;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;

import static java.util.Objects.requireNonNull;

public final class DeleteOption {
    public static final DeleteOption DEFAULT = builder().build();

    private final ByteSequence endKey;
    private final boolean prevKV;
    private final boolean prefix;
    private final boolean autoRetry;

    private DeleteOption(ByteSequence endKey, boolean prevKV, boolean prefix, final boolean autoRetry) {
        this.endKey = endKey;
        this.prevKV = prevKV;
        this.prefix = prefix;
        this.autoRetry = autoRetry;
    }

    public Optional<ByteSequence> getEndKey() {
        return Optional.ofNullable(endKey);
    }

    /**
     * Whether to get the previous key/value pairs before deleting them.
     *
     * @return true if get the previous key/value pairs before deleting them, otherwise false.
     */
    public boolean isPrevKV() {
        return prevKV;
    }

    /**
     * Whether to treat this deletion as deletion by prefix
     *
     * @return true if deletion by prefix.
     */
    public boolean isPrefix() {
        return prefix;
    }

    /**
     * Whether to treat a delete operation as idempotent from the point of view of automated retries.
     * Note under failure scenarios this may mean a single delete is attempted more than once.
     *
     * @return true if automated retries should happen.
     */
    public boolean isAutoRetry() {
        return autoRetry;
    }

    /**
     * Returns the builder.
     *
     * @deprecated use {@link #builder()}
     * @return     the builder
     */
    @SuppressWarnings("InlineMeSuggester")
    @Deprecated
    public static Builder newBuilder() {
        return builder();
    }

    /**
     * Returns the builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ByteSequence endKey;
        private boolean prevKV = false;
        private boolean prefix = false;
        private boolean autoRetry = false;

        private Builder() {
        }

        /**
         * Set the end key of the delete request. If it is set, the delete request will delete the keys
         * from <i>key</i> to <i>endKey</i> (exclusive).
         *
         * <p>
         * If end key is '\0', the range is all keys {@literal >=}
         * key.
         *
         * <p>
         * If the end key is one bit larger than the given key, then it deletes all keys with
         * the prefix (the given key).
         *
         * <p>
         * If both key and end key are '\0', it deletes all keys.
         *
         * @param  endKey end key
         * @return        builder
         */
        public Builder withRange(ByteSequence endKey) {
            this.endKey = endKey;
            return this;
        }

        /**
         * Enables 'Delete' requests to delete all the keys by prefix.
         *
         * <p>
         *
         * @param  prefix flag to delete all the keys by prefix
         * @return        builder
         */
        public DeleteOption.Builder isPrefix(boolean prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Enables 'Delete' requests to delete all the keys with matching prefix.
         *
         * <p>
         * You should pass the key that is passed into
         * {@link KV#delete(ByteSequence) KV.delete} method
         * into this method as the given key.
         *
         * @param      prefix the common prefix of all the keys that you want to delete
         * @return            builder
         * @deprecated        Use {@link #isPrefix(boolean)} instead.
         */
        @Deprecated
        public Builder withPrefix(ByteSequence prefix) {
            requireNonNull(prefix, "prefix should not be null");
            ByteSequence prefixEnd = OptionsUtil.prefixEndOf(prefix);
            this.withRange(prefixEnd);
            return this;
        }

        /**
         * Get the previous key/value pairs before deleting them.
         *
         * @param  prevKV flag to get previous key/value pairs before deleting them.
         * @return        builder
         */
        public Builder withPrevKV(boolean prevKV) {
            this.prevKV = prevKV;
            return this;
        }

        /**
         * When autoRetry is set, the delete operation is treated as idempotent from the point of view of automated retries.
         * Note under some failure scenarios true may make a delete operation be attempted more than once, where
         * a first attempt succeeded but its result did not reach the client; by default (autoRetry=false),
         * the client won't retry since it is not safe to assume on such a failure the operation did not happen.
         * Requesting withAutoRetry means the client is explicitly asking for retry nevertheless.
         *
         * @return builder
         */
        public Builder withAutoRetry() {
            this.autoRetry = true;
            return this;
        }

        public DeleteOption build() {
            return new DeleteOption(endKey, prevKV, prefix, autoRetry);
        }

    }
}
