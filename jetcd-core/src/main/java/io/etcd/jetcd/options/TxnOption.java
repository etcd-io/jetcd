package io.etcd.jetcd.options;

public final class TxnOption {
    public static final TxnOption DEFAULT = builder().build();

    private final boolean autoRetry;

    private TxnOption(final boolean autoRetry) {
        this.autoRetry = autoRetry;
    }

    /**
     * Whether to treat a txn operation as idempotent from the point of view of automated retries.
     *
     * @return true if automated retries should happen.
     */
    public boolean isAutoRetry() {
        return autoRetry;
    }

    /**
     * Returns the builder.
     *
     * @return the builder
     */
    public static TxnOption.Builder builder() {
        return new TxnOption.Builder();
    }

    public static final class Builder {
        private boolean autoRetry = false;

        private Builder() {
        }

        /**
         * When autoRetry is set, the txn operation is treated as idempotent from the point of view of automated retries.
         * Note under some failure scenarios true may make a txn operation be attempted and/or execute more than once, where
         * a first attempt executed but its result status did not reach the client; by default (autoRetry=false),
         * the client won't retry since it is not safe to assume on such a failure the operation did not happen.
         * Requesting withAutoRetry means the client is explicitly asking for retry nevertheless.
         *
         * @return builder
         */
        public Builder withAutoRetry() {
            this.autoRetry = true;
            return this;
        }

        public TxnOption build() {
            return new TxnOption(autoRetry);
        }
    }
}
