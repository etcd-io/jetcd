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

package io.etcd.jetcd.lock;

import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Util;

public class LockResponse extends AbstractResponse<io.etcd.jetcd.api.lock.LockResponse> {

    private final ByteSequence unprefixedKey;

    public LockResponse(io.etcd.jetcd.api.lock.LockResponse response, ByteSequence namespace) {
        super(response, response.getHeader());
        this.unprefixedKey = ByteSequence.from(Util.unprefixNamespace(getResponse().getKey(), namespace));
    }

    /**
     * @return the key that will exist on etcd for the duration that the Lock caller
     *         owns the lock. Users should not modify this key or the lock may exhibit
     *         undefined behavior.
     */
    public ByteSequence getKey() {
        return unprefixedKey;
    }

}
