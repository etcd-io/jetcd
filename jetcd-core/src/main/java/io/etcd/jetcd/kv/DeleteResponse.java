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

import java.util.List;
import java.util.stream.Collectors;

import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.api.DeleteRangeResponse;

public class DeleteResponse extends AbstractResponse<DeleteRangeResponse> {

    private final ByteSequence namespace;

    private List<KeyValue> prevKvs;

    public DeleteResponse(DeleteRangeResponse deleteRangeResponse, ByteSequence namespace) {
        super(deleteRangeResponse, deleteRangeResponse.getHeader());
        this.namespace = namespace;
    }

    /**
     * Returns the number of keys deleted by the delete range request.
     */
    public long getDeleted() {
        return getResponse().getDeleted();
    }

    /**
     * Returns previous key-value pairs.
     */
    public synchronized List<KeyValue> getPrevKvs() {
        if (prevKvs == null) {
            prevKvs = getResponse().getPrevKvsList().stream().map(kv -> new KeyValue(kv, namespace))
                .collect(Collectors.toList());
        }

        return prevKvs;
    }
}
