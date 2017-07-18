package com.coreos.jetcd.cluster;

import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;

/**
 * MemberRemoveResponse returned by {@link com.coreos.jetcd.Cluster#removeMember(long)}
 * contains a header and a list of member the removal of the member.
 */
public class MemberRemoveResponse extends
    AbstractResponse<com.coreos.jetcd.api.MemberRemoveResponse> {

  private List<Member> members;

  public MemberRemoveResponse(com.coreos.jetcd.api.MemberRemoveResponse response) {
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
