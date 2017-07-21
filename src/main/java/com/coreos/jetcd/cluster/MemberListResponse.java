package com.coreos.jetcd.cluster;

import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;

/**
 * MemberListResponse returned by {@link Cluster#listMember()}
 * contains a header and a list of members.
 */
public class MemberListResponse extends AbstractResponse<com.coreos.jetcd.api.MemberListResponse> {

  private List<Member> members;

  public MemberListResponse(com.coreos.jetcd.api.MemberListResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns a list of members. empty list if none.
   */
  public synchronized List<Member> getMembers() {
    if (members == null) {
      members = Util.toMembers(getResponse().getMembersList());
    }

    return members;
  }
}
