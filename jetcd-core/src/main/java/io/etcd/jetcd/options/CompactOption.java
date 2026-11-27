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
 * The option for compact operation.
 *
 * @param physical whether to wait for physical compaction
 */
public record CompactOption(
    boolean physical) {

    public static final CompactOption DEFAULT = builder().build();

    /**
     * Creates a new builder for CompactOption.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CompactOption.
     */
    public static final class Builder {
        private boolean physical = false;

        private Builder() {
        }

        /**
         * make compact RPC call wait until
         * the compaction is physically applied to the local database
         * such that compacted entries are totally removed from the
         * backend database.
         *
         * @param  physical whether the compact should wait until physically applied
         * @return          builder
         */
        public Builder withCompactPhysical(boolean physical) {
            this.physical = physical;
            return this;
        }

        /**
         * Builds the CompactOption.
         *
         * @return the CompactOption
         */
        public CompactOption build() {
            return new CompactOption(this.physical);
        }
    }
}
