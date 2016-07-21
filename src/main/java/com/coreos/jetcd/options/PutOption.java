package com.coreos.jetcd.options;

public class PutOption {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private long leaseId = 0L;

        private Builder() {}

        public Builder withLeaseId(long leaseId) {
            this.leaseId = leaseId;
            return this;
        }

        public PutOption build() {
            return new PutOption(leaseId);
        }

    }

    private final long leaseId;

    private PutOption(long leaseId) {
        this.leaseId = leaseId;
    }

}
