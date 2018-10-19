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
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.api.DeleteRangeResponse;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteResponse extends AbstractResponse<DeleteRangeResponse> {

  private List<KeyValue> prevKvs;

  public DeleteResponse(DeleteRangeResponse deleteRangeResponse) {
    super(deleteRangeResponse, deleteRangeResponse.getHeader());
  }

  /**
   * return the number of keys deleted by the delete range request.
   */
  public long getDeleted() {
    return getResponse().getDeleted();
  }

  /**
   * return previous key-value pairs.
   */
  public synchronized List<KeyValue> getPrevKvs() {
    if (prevKvs == null) {
      prevKvs = getResponse().getPrevKvsList().stream()
          .map(KeyValue::new)
          .collect(Collectors.toList());
    }

    return prevKvs;
  }
}
