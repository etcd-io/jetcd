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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The options for put operation.
 */
public final class PutOption {
    public static final PutOption DEFAULT = builder().build();

    private final long leaseId;
    private final boolean prevKV;
    private final boolean autoRetry;

    private PutOption(long leaseId, boolean prevKV, boolean autoRetry) {
        this.leaseId = leaseId;
        this.prevKV = prevKV;
        this.autoRetry = autoRetry;
    }

    /**
     * Get the lease id.
     *
     * @return the lease id
     */
    public long getLeaseId() {
        return this.leaseId;
    }

    /**
     * Get the previous KV.
     *
     * @return the prevKV
     */
    public boolean getPrevKV() {
        return this.prevKV;
    }

    /**
     * Whether to treat a put operation as idempotent from the point of view of automated retries.
     * Note under failure scenarios this may mean a single put executes more than once.
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

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to construct a put option.
     */
    public static final class Builder {

        private long leaseId = 0L;
        private boolean prevKV = false;
        private boolean autoRetry = false;

        private Builder() {
        }

        /**
         * Assign a <i>leaseId</i> for a put operation. Zero means no lease.
         *
         * @param  leaseId                  lease id to apply to a put operation
         * @return                          builder
         * @throws IllegalArgumentException if lease is less than zero.
         */
        public Builder withLeaseId(long leaseId) {
            checkArgument(leaseId >= 0, "leaseId should greater than or equal to zero: leaseId=%s", leaseId);
            this.leaseId = leaseId;
            return this;
        }

        /**
         * When withPrevKV is set, put response contains previous key-value pair.
         *
         * @return builder
         */
        public Builder withPrevKV() {
            this.prevKV = true;
            return this;
        }

        /**
         * When autoRetry is set, treat this put as idempotent from the point of view of automated retries.
         * Note under some failure scenarios autoRetry=true may make a put operation execute more than once, where
         * a first attempt succeeded but its result did not reach the client; by default (autoRetry=false),
         * the client won't retry since it is not safe to assume on such a failure that the operation did not happen
         * in the server.
         * Requesting withAutoRetry means the client is explicitly asking for retry nevertheless.
         *
         * @return builder
         */
        public Builder withAutoRetry() {
            this.autoRetry = true;
            return this;
        }

        /**
         * build the put option.
         *
         * @return the put option
         */
        public PutOption build() {
            return new PutOption(this.leaseId, this.prevKV, this.autoRetry);
        }

    }
}
