package com.coreos.jetcd.options;

import java.util.Optional;

import com.coreos.jetcd.api.RangeRequest;
import com.google.protobuf.ByteString;

/**
 * The option for get operation.
 */
public final class GetOption {

    public static final GetOption DEFAULT = newBuilder().build();

    /**
     * Create a builder to construct option for get operation.
     *
     * @return builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private long                    limit        = 0L;
        private long                    revision     = 0L;
        private RangeRequest.SortOrder  sortOrder    = RangeRequest.SortOrder.NONE;
        private RangeRequest.SortTarget sortTarget   = RangeRequest.SortTarget.KEY;
        private boolean                 serializable = false;
        private boolean                 keysOnly     = false;
        private boolean                 countOnly    = false;
        private Optional<ByteString>    endKey       = Optional.empty();

        private Builder() {
        }

        /**
         * Limit the number of keys to return for a get request. By default is 0 - no limitation.
         *
         * @param limit the maximum number of keys to return for a get request.
         * @return builder
         */
        public Builder withLimit(long limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Provide the revision to use for the get request.
         * <p>If the revision is less or equal to zero, the get is over the newest key-value store.
         * <p>If the revision has been compacted, ErrCompacted is returned as a response.
         *
         * @param revision the revision to get.
         * @return builder
         */
        public Builder withRevision(long revision) {
            this.revision = revision;
            return this;
        }

        /**
         * Sort the return key value pairs in the provided <i>order</i>.
         *
         * @param order order to sort the returned key value pairs.
         * @return builder
         */
        public Builder withSortOrder(RangeRequest.SortOrder order) {
            this.sortOrder = order;
            return this;
        }

        /**
         * Sort the return key value pairs in the provided <i>field</i>.
         *
         * @param field field to sort the key value pairs by the provided {@link #withSortOrder(RangeRequest.SortOrder)}.
         * @return builder
         */
        public Builder withSortField(RangeRequest.SortTarget field) {
            this.sortTarget = field;
            return this;
        }

        /**
         * Set the get request to be a serializable get request.
         * <p>Get requests are linearizable by default. For better performance, a serializable get request
         * is served locally without needing to reach consensus with other nodes in the cluster.
         *
         * @param serializable is the get request a serializable get request.
         * @return builder
         */
        public Builder withSerializable(boolean serializable) {
            this.serializable = serializable;
            return this;
        }

        /**
         * Set the get request to only return keys
         *
         * @param keysOnly flag to only return keys
         * @return builder
         */
        public Builder withKeysOnly(boolean keysOnly) {
            this.keysOnly = keysOnly;
            return this;
        }

        /**
         * Set the get request to only return count of the keys
         *
         * @param countOnly flag to only return count of the keys
         * @return builder
         */
        public Builder withCountOnly(boolean countOnly) {
            this.countOnly = countOnly;
            return this;
        }

        /**
         * Set the end key of the get request. If it is set, the
         * get request will return the keys from <i>key</i> to <i>endKey</i> (exclusive).
         * <p>If end key is '\0', the range is all keys >= key.
         * <p>If the end key is one bit larger than the given key, then it gets all keys with the prefix (the given key).
         * <p>If both key and end key are '\0', it returns all keys.
         *
         * @param endKey end key
         * @return builder
         */
        public Builder withRange(ByteString endKey) {
            this.endKey = Optional.ofNullable(endKey);
            return this;
        }

        public GetOption build() {
            return new GetOption(endKey, limit, revision, sortOrder, sortTarget, serializable, keysOnly, countOnly);
        }

    }

    private final Optional<ByteString>    endKey;
    private final long                    limit;
    private final long                    revision;
    private final RangeRequest.SortOrder  sortOrder;
    private final RangeRequest.SortTarget sortTarget;
    private final boolean                 serializable;
    private final boolean                 keysOnly;
    private final boolean                 countOnly;

    private GetOption(Optional<ByteString> endKey, long limit, long revision, RangeRequest.SortOrder sortOrder,
                      RangeRequest.SortTarget sortTarget, boolean serializable, boolean keysOnly, boolean countOnly) {
        this.endKey = endKey;
        this.limit = limit;
        this.revision = revision;
        this.sortOrder = sortOrder;
        this.sortTarget = sortTarget;
        this.serializable = serializable;
        this.keysOnly = keysOnly;
        this.countOnly = countOnly;
    }

    /**
     * Get the maximum number of keys to return for a get request.
     *
     * @return the maximum number of keys to return.
     */
    public long getLimit() {
        return this.limit;
    }

    public Optional<ByteString> getEndKey() {
        return this.endKey;
    }

    public long getRevision() {
        return revision;
    }

    public RangeRequest.SortOrder getSortOrder() {
        return sortOrder;
    }

    public RangeRequest.SortTarget getSortField() {
        return sortTarget;
    }

    public boolean isSerializable() {
        return serializable;
    }

    public boolean isKeysOnly() {
        return keysOnly;
    }

    public boolean isCountOnly() {
        return countOnly;
    }
}
