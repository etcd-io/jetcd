/*
 * Copyright 2016-2020 The jetcd authors
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

import java.util.List;
import java.util.stream.Collectors;

import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.api.RangeResponse;

public class GetResponse extends AbstractResponse<RangeResponse> {

    private final ByteSequence namespace;

    private List<KeyValue> kvs;

    public GetResponse(RangeResponse rangeResponse, ByteSequence namespace) {
        super(rangeResponse, rangeResponse.getHeader());
        this.namespace = namespace;
    }

    /**
     * return a list of key-value pairs matched by the range request.
     */
    public synchronized List<KeyValue> getKvs() {
        if (kvs == null) {
            kvs = getResponse().getKvsList().stream().map(kv -> new KeyValue(kv, namespace)).collect(Collectors.toList());
        }

        return kvs;
    }

    /**
     * more indicates if there are more keys to return in the requested range.
     */
    public boolean isMore() {
        return getResponse().getMore();
    }

    /**
     * return the number of keys within the range when requested.
     */
    public long getCount() {
        return getResponse().getCount();
    }
}
