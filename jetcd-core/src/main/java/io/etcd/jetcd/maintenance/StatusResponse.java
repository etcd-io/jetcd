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
 * StatusResponse returned by {@link Maintenance#statusMember(String)} contains
 * a header, version, dbSize, current leader, raftIndex, and raftTerm.
 */
public class StatusResponse extends AbstractResponse<io.etcd.jetcd.api.StatusResponse> {

  public StatusResponse(io.etcd.jetcd.api.StatusResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns the cluster protocol version used by the responding member.
   */
  public String getVersion() {
    return getResponse().getVersion();
  }

  /**
   * return the size of the backend database, in bytes, of the responding member.
   */
  public long getDbSize() {
    return getResponse().getDbSize();
  }

  /**
   * return the the member ID which the responding member believes is the current leader.
   */
  public long getLeader() {
    return getResponse().getLeader();
  }

  /**
   * the current raft index of the responding member.
   */
  public long getRaftIndex() {
    return getResponse().getRaftIndex();
  }

  /**
   * the current raft term of the responding member.
   */
  public long getRaftTerm() {
    return getResponse().getRaftTerm();
  }
}
