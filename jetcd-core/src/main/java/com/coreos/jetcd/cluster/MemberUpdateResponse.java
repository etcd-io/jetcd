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

package com.coreos.jetcd.cluster;

import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;

/**
 * MemberUpdateResponse returned by {@link com.coreos.jetcd.Cluster#updateMember(long, List)}
 * contains a header and a list of members after the member update.
 */
public class MemberUpdateResponse extends
    AbstractResponse<com.coreos.jetcd.api.MemberUpdateResponse> {

  private List<Member> members;

  public MemberUpdateResponse(com.coreos.jetcd.api.MemberUpdateResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns a list of all members after updating the member.
   */
  public synchronized List<Member> getMembers() {
    if (members == null) {
      members = Util.toMembers(getResponse().getMembersList());
    }

    return members;
  }
}
