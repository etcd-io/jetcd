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

package io.etcd.jetcd.cluster;

import io.etcd.jetcd.AbstractResponse;
import java.util.List;

/**
 * MemberRemoveResponse returned by {@link io.etcd.jetcd.Cluster#removeMember(long)}
 * contains a header and a list of member the removal of the member.
 */
public class MemberRemoveResponse extends
    AbstractResponse<io.etcd.jetcd.api.MemberRemoveResponse> {

  private List<Member> members;

  public MemberRemoveResponse(io.etcd.jetcd.api.MemberRemoveResponse response) {
    super(response, response.getHeader());
  }


  /**
   * returns a list of all members after removing the member.
   */
  public synchronized List<Member> getMembers() {
    if (members == null) {
      members = Util.toMembers(getResponse().getMembersList());
    }

    return members;
  }
}
