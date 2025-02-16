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
import io.etcd.jetcd.kv.GetResponse;

import static java.util.Objects.requireNonNull;

/**
 * The option for get operation.
 */
public final class GetOption {
    public static final GetOption DEFAULT = builder().build();

    private final ByteSequence endKey;
    private final long limit;
    private final long revision;
    private final SortOrder sortOrder;
    private final SortTarget sortTarget;
    private final boolean serializable;
    private final boolean keysOnly;
    private final boolean countOnly;
    private final long minCreateRevision;
    private final long maxCreateRevision;
    private final long minModRevision;
    private final long maxModRevision;
    private final boolean prefix;

    private GetOption(
        ByteSequence endKey,
        long limit,
        long revision,
        SortOrder sortOrder,
        SortTarget sortTarget,
        boolean serializable,
        boolean keysOnly,
        boolean countOnly,
        long minCreateRevision,
        long maxCreateRevision,
        long minModRevision,
        long maxModRevision,
        boolean prefix) {

        this.endKey = endKey;
        this.limit = limit;
        this.revision = revision;
        this.sortOrder = sortOrder;
        this.sortTarget = sortTarget;
        this.serializable = serializable;
        this.keysOnly = keysOnly;
        this.countOnly = countOnly;
        this.minCreateRevision = minCreateRevision;
        this.maxCreateRevision = maxCreateRevision;
        this.minModRevision = minModRevision;
        this.maxModRevision = maxModRevision;
        this.prefix = prefix;
    }

    /**
     * Get the maximum number of keys to return for a get request.
     *
     * <p>
     * Note this filter does not affect the count field in GetResponse.
     * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
     * </p>
     *
     *
     * @return the maximum number of keys to return.
     */
    public long getLimit() {
        return this.limit;
    }

    /**
     * Get the end key for a range request
     *
     * @return the end key for a range request
     */
    public Optional<ByteSequence> getEndKey() {
        return Optional.ofNullable(this.endKey);
    }

    /**
     * Get the revision for the request
     *
     * @return the revision for the request
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Get the sort order for the request
     *
     * @return the sort order for the request
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * Get the sort field for the request
     *
     * @return the sort field for the request
     */
    public SortTarget getSortField() {
        return sortTarget;
    }

    /**
     * Return if the consistency level for this request is "serializable".
     * Note serializable is a lower than default consistency, and implies
     * the possibility of getting stale data.
     *
     * @return true if this request is only serializable consistency
     */
    public boolean isSerializable() {
        return serializable;
    }

    /**
     * True if this request should get only keys in a range and there is no
     * need to retrieve the values.
     *
     * @return true if only get keys
     */
    public boolean isKeysOnly() {
        return keysOnly;
    }

    /**
     * True if this request should only populate the count of keys matching
     * a range, and no other data.
     *
     * @return true if only get the count of keys
     */
    public boolean isCountOnly() {
        return countOnly;
    }

    /**
     * Only populate results for keys that match a
     * minimum value for a created revision.
     *
     * <p>
     * Note this filter does not affect the count field in GetResponse.
     * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
     * For the same reason, it would be meaningless to mix setting a min create revision option
     * with the count only option.
     * </p>
     *
     * @return minimum created revision to match, or zero for any.
     */
    public long getMinCreateRevision() {
        return this.minCreateRevision;
    }

    /**
     * Only populate results for keys that match a
     * maximum value for a created revision.
     *
     * <p>
     * Note this filter does not affect the count field in GetResponse.
     * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
     * For the same reason, it would be meaningless to mix setting a max create revision option
     * with the count only option.
     * </p>
     *
     * @return maximum created revision to match, or zero for any.
     */
    public long getMaxCreateRevision() {
        return this.maxCreateRevision;
    }

    /**
     * Only populate results for keys that match a
     * minimum value for a modified revision.
     *
     * <p>
     * Note this filter does not affect the count field in GetResponse.
     * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
     * For the same reason, it would be meaningless to mix setting a min mod revision option
     * with the count only option.
     * </p>
     *
     * @return minimum modified revision to match, or zero for any.
     */
    public long getMinModRevision() {
        return this.minModRevision;
    }

    /**
     * Only populate results for keys that match a
     * maximum value for a modified revision.
     *
     * <p>
     * Note this filter does not affect the count field in GetResponse.
     * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
     * For the same reason, it would be meaningless to mix setting a max mod revision option
     * with the count only option.
     * </p>
     *
     * @return maximum modified revision to match, or zero for any.
     */
    public long getMaxModRevision() {
        return this.maxModRevision;
    }

    /**
     * True if this Get request should do prefix match
     *
     * @return true if this Get request should do prefix match
     */
    public boolean isPrefix() {
        return prefix;
    }

    public enum SortOrder {
        NONE, ASCEND, DESCEND,
    }

    public enum SortTarget {
        KEY, VERSION, CREATE, MOD, VALUE,
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private long limit = 0L;
        private long revision = 0L;
        private SortOrder sortOrder = SortOrder.NONE;
        private SortTarget sortTarget = SortTarget.KEY;
        private boolean serializable = false;
        private boolean keysOnly = false;
        private boolean countOnly = false;
        private ByteSequence endKey;
        private long minCreateRevision = 0L;
        private long maxCreateRevision = 0L;
        private long minModRevision = 0L;
        private long maxModRevision = 0L;
        private boolean prefix = false;

        private Builder() {
        }

        /**
         * Limit the number of keys to return for a get request. By default is 0 - no limitation.
         *
         * <p>
         * Note this filter does not affect the count field in GetResponse.
         * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
         * For the same reason, it would be meaningless to mix setting this option
         * with the count only option.
         * </p>
         *
         * @param  limit the maximum number of keys to return for a get request.
         * @return       builder
         */
        public Builder withLimit(long limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Provide the revision to use for the get request.
         *
         * <p>
         * If the revision is less or equal to zero, the get is over the newest key-value store.
         *
         * <p>
         * If the revision has been compacted, ErrCompacted is returned as a response.
         *
         * @param  revision the revision to get.
         * @return          builder
         */
        public Builder withRevision(long revision) {
            this.revision = revision;
            return this;
        }

        /**
         * Sort the return key value pairs in the provided <i>order</i>.
         *
         * @param  order order to sort the returned key value pairs.
         * @return       builder
         */
        public Builder withSortOrder(SortOrder order) {
            this.sortOrder = order;
            return this;
        }

        /**
         * Sort the return key value pairs in the provided <i>field</i>.
         *
         * @param  field field to sort the key value pairs by the provided
         * @return       builder
         */
        public Builder withSortField(SortTarget field) {
            this.sortTarget = field;
            return this;
        }

        /**
         * Set the get request to be a serializable get request.
         *
         * <p>
         * Get requests are linearizable by
         * default. For better performance, a serializable get request is served locally without needing
         * to reach consensus with other nodes in the cluster. Note this is a tradeoff with strict
         * consistency so it should be used with care in situations where reading stale
         * is acceptable.
         *
         * @param  serializable is the get request a serializable get request.
         * @return              builder
         */
        public Builder withSerializable(boolean serializable) {
            this.serializable = serializable;
            return this;
        }

        /**
         * Set the get request to only return keys.
         *
         * @param  keysOnly flag to only return keys
         * @return          builder
         */
        public Builder withKeysOnly(boolean keysOnly) {
            this.keysOnly = keysOnly;
            return this;
        }

        /**
         * Set the get request to only return count of the keys.
         *
         * @param  countOnly flag to only return count of the keys
         * @return           builder
         */
        public Builder withCountOnly(boolean countOnly) {
            this.countOnly = countOnly;
            return this;
        }

        /**
         * Set the end key of the get request. If it is set, the get request will return the keys from
         * <i>key</i> to <i>endKey</i> (exclusive).
         *
         * <p>
         * If end key is '\0', the range is all keys {@literal >=} key.
         *
         * <p>
         * If the end key is one bit larger than the given key, then it gets all keys with the
         * prefix (the given key).
         *
         * <p>
         * If both key and end key are '\0', it returns all keys.
         *
         * @param  endKey end key
         * @return        builder
         */
        public Builder withRange(ByteSequence endKey) {
            this.endKey = endKey;
            return this;
        }

        /**
         * Enables 'Get' requests to obtain all the keys by prefix.
         *
         * <p>
         *
         * @param  prefix flag to obtain all the keys by prefix
         * @return        builder
         */
        public Builder isPrefix(boolean prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Enables 'Get' requests to obtain all the keys with matching prefix.
         *
         * <p>
         * You should pass the key that is passed into
         * {@link KV#get(ByteSequence) KV.get} method into this method as the given key.
         *
         * @param      prefix the common prefix of all the keys that you want to get
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
         * Limit returned keys to those with create revision greater or equal than the provided value.
         * min_create_revision is the lower bound for returned key create revisions; all keys with
         * lesser create revisions will be filtered away.
         *
         * <p>
         * Note this filter does not affect the count field in GetResponse.
         * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
         * For the same reason, it would be meaningless to mix setting this option
         * with the count only option.
         * </p>
         *
         * @param  createRevision create revision
         * @return                builder
         */
        public Builder withMinCreateRevision(long createRevision) {
            this.minCreateRevision = createRevision;
            return this;
        }

        /**
         * Limit returned keys to those with create revision less or equal than the provided value.
         * max_create_revision is the upper bound for returned key create revisions; all keys with
         * greater create revisions will be filtered away.
         *
         * <p>
         * Note this filter does not affect the count field in GetResponse.
         * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
         * For the same reason, it would be meaningless to mix setting this option
         * with the count only option.
         * </p>
         *
         * @param  createRevision create revision
         * @return                builder
         */
        public Builder withMaxCreateRevision(long createRevision) {
            this.maxCreateRevision = createRevision;
            return this;
        }

        /**
         * Limit returned keys to those with mod revision greater or equal than the provided value.
         * min_mod_revision is the lower bound for returned key mod revisions; all keys with lesser mod
         * revisions will be filtered away.
         *
         * <p>
         * Note this filter does not affect the count field in GetResponse.
         * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
         * For the same reason, it would be meaningless to mix setting this option
         * with the count only option.
         * </p>
         *
         * @param  modRevision mod revision
         * @return             this builder instance
         */
        public Builder withMinModRevision(long modRevision) {
            this.minModRevision = modRevision;
            return this;
        }

        /**
         * Limit returned keys to those with mod revision less or equal than the provided value. max_mod_revision
         * is the upper bound for returned key mod revisions; all keys with greater mod revisions will
         * be filtered away.
         *
         * <p>
         * Note this filter does not affect the count field in GetResponse.
         * {@link GetResponse#getCount()} always counts the number of keys matched on a range, independent of filters.
         * For the same reason, it would be meaningless to mix setting this option
         * with the count only option.
         * </p>
         *
         * @param  modRevision mod revision
         * @return             this builder instance
         */
        public Builder withMaxModRevision(long modRevision) {
            this.maxModRevision = modRevision;
            return this;
        }

        /**
         * Build the GetOption.
         *
         * @return the GetOption
         */
        public GetOption build() {
            return new GetOption(
                endKey,
                limit,
                revision,
                sortOrder,
                sortTarget,
                serializable,
                keysOnly,
                countOnly,
                minCreateRevision,
                maxCreateRevision,
                minModRevision,
                maxModRevision,
                prefix);
        }

    }
}
