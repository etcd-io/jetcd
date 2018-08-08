/**
 * Copyright 2017 The jetcd authors
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

package io.etcd.jetcd.maintenance;

import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.data.AbstractResponse;

/**
 * HashKVResponse returned by {@link Maintenance#HashKV(String, long)}.
 */
public class HashKVResponse extends AbstractResponse<io.etcd.jetcd.api.HashKVResponse> {

  public HashKVResponse(io.etcd.jetcd.api.HashKVResponse response) {
    super(response, response.getHeader());
  }

  /**
   * return the hash value computed from the responding member's MVCC keys up to a given revision.
   */
  public int getHash() {
    return getResponse().getHash();
  }

  /**
   * return compact_revision is the compacted revision of key-value store when hash begins.
   */
  public long getCompacted() {
    return getResponse().getCompactRevision();
  }
}
