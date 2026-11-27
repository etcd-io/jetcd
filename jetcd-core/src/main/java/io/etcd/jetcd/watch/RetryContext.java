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

package io.etcd.jetcd.watch;

import java.time.Duration;

/**
 * Context information about a retry attempt.
 *
 * @param type         type of retry operation
 * @param attemptCount current attempt number
 * @param maxAttempts  maximum number of attempts
 * @param nextDelay    delay until next retry
 * @param cause        exception that triggered the retry
 */
public record RetryContext(
    RetryType type,
    int attemptCount,
    int maxAttempts,
    Duration nextDelay,
    Throwable cause) {

    /**
     * The type of operation being retried.
     */
    public enum RetryType {
        /**
         * Stream resume retry (connection/stream failure).
         */
        RESUME,
        /**
         * Progress request retry (write stream not ready).
         */
        PROGRESS_REQUEST
    }

    /**
     * True if this is the last retry attempt.
     *
     * @return true if this is the last attempt
     */
    public boolean isLastAttempt() {
        return attemptCount >= maxAttempts;
    }

    /**
     * Creates a builder for the specified retry type.
     *
     * @param  type the retry type
     * @return      a new builder
     */
    public static Builder of(RetryType type) {
        return new Builder(type);
    }

    /**
     * Builder for RetryContext.
     */
    public static final class Builder {
        private final RetryType type;
        private int attemptCount;
        private int maxAttempts;
        private Duration nextDelay;
        private Throwable cause;

        private Builder(RetryType type) {
            this.type = type;
        }

        /**
         * Sets the attempt count.
         *
         * @param  attemptCount the attempt count
         * @return              this builder
         */
        public Builder attemptCount(int attemptCount) {
            this.attemptCount = attemptCount;
            return this;
        }

        /**
         * Sets the maximum attempts.
         *
         * @param  maxAttempts the maximum attempts
         * @return             this builder
         */
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Sets the next delay.
         *
         * @param  nextDelay the next delay
         * @return           this builder
         */
        public Builder nextDelay(Duration nextDelay) {
            this.nextDelay = nextDelay;
            return this;
        }

        /**
         * Sets the cause.
         *
         * @param  cause the cause
         * @return       this builder
         */
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Builds the RetryContext.
         *
         * @return the retry context
         */
        public RetryContext build() {
            return new RetryContext(type, attemptCount, maxAttempts, nextDelay, cause);
        }
    }
}
