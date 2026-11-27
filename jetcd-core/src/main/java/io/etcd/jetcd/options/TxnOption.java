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

/**
 * Options for transaction operations.
 *
 * @param autoRetry whether to automatically retry
 */
public record TxnOption(
    boolean autoRetry) {

    /** Default transaction option. */
    public static final TxnOption DEFAULT = builder().build();

    /**
     * Whether to treat a txn operation as idempotent from the point of view of automated retries.
     *
     * @return true if automated retries should happen.
     */
    @Override
    public boolean autoRetry() {
        return autoRetry;
    }

    /**
     * Returns the builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TxnOption.
     */
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

        /**
         * Builds the TxnOption.
         *
         * @return the txn option
         */
        public TxnOption build() {
            return new TxnOption(autoRetry);
        }
    }
}
