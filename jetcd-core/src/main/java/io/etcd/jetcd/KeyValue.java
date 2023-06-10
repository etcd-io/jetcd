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

package io.etcd.jetcd;

import io.etcd.jetcd.support.Util;

/**
 * Etcd key value pair.
 */
public class KeyValue {

    private final io.etcd.jetcd.api.KeyValue kv;
    private final ByteSequence unprefixedKey;
    private final ByteSequence value;

    public KeyValue(io.etcd.jetcd.api.KeyValue kv, ByteSequence namespace) {
        this.kv = kv;
        this.value = ByteSequence.from(kv.getValue());

        this.unprefixedKey = ByteSequence.from(
            kv.getKey().isEmpty()
                ? kv.getKey()
                : Util.unprefixNamespace(kv.getKey(), namespace));
    }

    /**
     * Returns the key
     *
     * @return the key.
     */
    public ByteSequence getKey() {
        return unprefixedKey;
    }

    /**
     * Returns the value
     *
     * @return the value.
     */
    public ByteSequence getValue() {
        return value;
    }

    /**
     * Returns the create revision.
     *
     * @return the create revision.
     */
    public long getCreateRevision() {
        return kv.getCreateRevision();
    }

    /**
     * Returns the mod revision.
     *
     * @return the mod revision.
     */
    public long getModRevision() {
        return kv.getModRevision();
    }

    /**
     * Returns the version.
     *
     * @return the version.
     */
    public long getVersion() {
        return kv.getVersion();
    }

    /**
     * Returns the lease.
     *
     * @return the lease.
     */
    public long getLease() {
        return kv.getLease();
    }
}
