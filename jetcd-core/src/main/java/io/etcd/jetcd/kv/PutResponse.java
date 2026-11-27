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

package io.etcd.jetcd.kv;

import java.util.Optional;
import java.util.function.Supplier;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.common.suppliers.Suppliers;
import io.etcd.jetcd.impl.AbstractResponse;

/**
 * Response from a put operation.
 */
public class PutResponse extends AbstractResponse<io.etcd.jetcd.api.PutResponse> {

    private final Supplier<Optional<KeyValue>> prevKv;

    /**
     * Creates a new PutResponse from the gRPC response.
     *
     * @param putResponse the gRPC put response
     * @param namespace   the namespace used
     */
    public PutResponse(io.etcd.jetcd.api.PutResponse putResponse, ByteSequence namespace) {
        super(putResponse, putResponse.getHeader());

        this.prevKv = Suppliers.memoizingOptional(() -> getResponse().hasPrevKv()
            ? new KeyValue(getResponse().getPrevKv(), namespace)
            : null);
    }

    /**
     * Returns previous key-value pair if present.
     *
     * @return Optional containing prev kv, or empty if not present
     */
    public Optional<KeyValue> getPrevKv() {
        return prevKv.get();
    }

    /**
     * Returns whether a previous key-value pair is present.
     *
     * @return     if has prev kv
     * @deprecated Use {@link #getPrevKv()}.isPresent() instead
     */
    @Deprecated
    public boolean hasPrevKv() {
        return getResponse().hasPrevKv();
    }
}
