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

public class LeaseOption {

    public static final LeaseOption DEFAULT = newBuilder().build();

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private boolean attachedKeys;

        private Builder() {
        }

        /**
         * requests lease timeToLive API to return attached keys of given lease ID.
         *
         * @return builder.
         */
        public Builder withAttachedKeys() {
            this.attachedKeys = true;
            return this;
        }

        public LeaseOption build() {
            return new LeaseOption(this.attachedKeys);
        }
    }

    private final boolean attachedKeys;

    private LeaseOption(boolean attachedKeys) {
        this.attachedKeys = attachedKeys;
    }

    public boolean isAttachedKeys() {
        return attachedKeys;
    }

}
