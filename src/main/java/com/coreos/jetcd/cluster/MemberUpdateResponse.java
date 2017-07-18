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
