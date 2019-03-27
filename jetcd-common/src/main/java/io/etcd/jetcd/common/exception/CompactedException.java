/*
 * Copyright 2016-2019 The jetcd authors
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

package io.etcd.jetcd.common.exception;

/**
 * CompactedException is thrown when a operation wants to retrieve key at a revision that has
 * been compacted.
 */
public class CompactedException extends EtcdException {

  private long compactedRevision;

  CompactedException(ErrorCode code, String message, long compactedRev) {
    super(code, message, null);
    this.compactedRevision = compactedRev;
  }

  // get the current compacted revision of etcd server.
  public long getCompactedRevision() {
    return compactedRevision;
  }
}
