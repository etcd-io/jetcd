package com.coreos.jetcd.options;

/**
 * The options for put operation.
 */
public class PutOption {

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder to construct a put option
     */
    public static class Builder {

        private long leaseId = 0L;

        private Builder() {}

        /**
         * Assign a <i>leaseId</i> for a put operation. Zero means no lease.
         *
         * @param leaseId lease id to apply to a put operation
         * @return builder
         */
        public Builder withLeaseId(long leaseId) {
            this.leaseId = leaseId;
            return this;
        }

        /**
         * build the put option.
         *
         * @return the put option.
         */
        public PutOption build() {
            return new PutOption(leaseId);
        }

    }

    private final long leaseId;

    private PutOption(long leaseId) {
        this.leaseId = leaseId;
    }

    /**
     * Get the lease id.
     *
     * @return the lease id
     */
    public long getLeaseId() {
        return leaseId;
    }

}
