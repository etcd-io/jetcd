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

package io.etcd.jetcd.kv;

import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;

public class PutResponse extends AbstractResponse<io.etcd.jetcd.api.PutResponse> {

  private final ByteSequence namespace;

  public PutResponse(io.etcd.jetcd.api.PutResponse putResponse, ByteSequence namespace) {
    super(putResponse, putResponse.getHeader());
    this.namespace = namespace;
  }

  /**
   * return previous key-value pair.
   */
  public KeyValue getPrevKv() {
    return new KeyValue(getResponse().getPrevKv(), namespace);
  }

  public boolean hasPrevKv() {
    return getResponse().hasPrevKv();
  }
}
