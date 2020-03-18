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

package io.etcd.jetcd.election;

import com.google.protobuf.ByteString;

public class LeaderKey {
    private final ByteString name;
    private final ByteString key;
    private final long revision;
    private final long lease;

    public LeaderKey(ByteString name, ByteString key, long revision, long lease) {
        this.name = name;
        this.key = key;
        this.revision = revision;
        this.lease = lease;
    }

    /**
     * election identifier that corresponds to the leadership key.
     */
    public ByteString getName() {
        return name;
    }

    /**
     * opaque key representing the ownership of the election. If the key
     * is deleted, then leadership is lost.
     */
    public ByteString getKey() {
        return key;
    }

    /**
     * creation revision of the key. It can be used to test for ownership
     * of an election during transactions by testing the key's creation revision
     * matches rev.
     */
    public long getRevision() {
        return revision;
    }

    /**
     * lease ID of the election leader.
     */
    public long getLease() {
        return lease;
    }
}
