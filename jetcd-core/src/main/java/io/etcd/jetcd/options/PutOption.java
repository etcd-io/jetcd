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

import static io.etcd.jetcd.common.Preconditions.checkArgument;

/**
 * The options for put operation.
 *
 * @param leaseId     lease ID to associate with the key
 * @param prevKV      whether to return previous key-value
 * @param autoRetry   whether to automatically retry on failure
 * @param ignoreValue if true, update key using current value (error if key does not exist)
 * @param ignoreLease if true, update key using current lease (error if key does not exist)
 */
public record PutOption(
    long leaseId,
    boolean prevKV,
    boolean autoRetry,
    boolean ignoreValue,
    boolean ignoreLease) {

    public static final PutOption DEFAULT = builder().build();

    /**
     * Get the lease id.
     *
     * @return the lease id
     */
    @Override
    public long leaseId() {
        return this.leaseId;
    }

    /**
     * Get the previous KV.
     *
     * @return the prevKV
     */
    @Override
    public boolean prevKV() {
        return this.prevKV;
    }

    /**
     * Whether to treat a put operation as idempotent from the point of view of automated retries.
     * Note under failure scenarios this may mean a single put executes more than once.
     *
     * @return true if automated retries should happen.
     */
    @Override
    public boolean autoRetry() {
        return autoRetry;
    }

    /**
     * If true, etcd updates the key using its current value.
     * Returns an error if the key does not exist.
     *
     * @return true if key should be updated using current value
     */
    @Override
    public boolean ignoreValue() {
        return ignoreValue;
    }

    /**
     * If true, etcd updates the key using its current lease.
     * Returns an error if the key does not exist.
     *
     * @return true if key should be updated using current lease
     */
    @Override
    public boolean ignoreLease() {
        return ignoreLease;
    }

    /**
     * Creates a new builder for PutOption.
     *
     * @return the builder
     */
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
        private boolean ignoreValue = false;
        private boolean ignoreLease = false;

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
            checkArgument(leaseId >= 0, "leaseId should greater than or equal to zero: leaseId=" + leaseId);
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
         * When ignoreValue is set, etcd updates the key using its current value.
         * This is useful when you want to update only the lease without changing the value.
         * Returns an error if the key does not exist.
         *
         * @return builder
         */
        public Builder withIgnoreValue() {
            this.ignoreValue = true;
            return this;
        }

        /**
         * When ignoreLease is set, etcd updates the key using its current lease.
         * This is useful when you want to update only the value without changing the lease.
         * Returns an error if the key does not exist.
         *
         * @return builder
         */
        public Builder withIgnoreLease() {
            this.ignoreLease = true;
            return this;
        }

        /**
         * build the put option.
         *
         * @return the put option
         */
        public PutOption build() {
            return new PutOption(this.leaseId, this.prevKV, this.autoRetry, this.ignoreValue, this.ignoreLease);
        }

    }
}
