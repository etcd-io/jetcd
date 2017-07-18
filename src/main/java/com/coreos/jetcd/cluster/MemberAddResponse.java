package com.coreos.jetcd.cluster;

import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;

/**
 * MemberAddResponse returned by {@link com.coreos.jetcd.Cluster#addMember(List)}
 * contains a header, added member, and list of members after adding the new member.
 */
public class MemberAddResponse extends AbstractResponse<com.coreos.jetcd.api.MemberAddResponse> {

  private final Member member;
  private List<Member> members;

  public MemberAddResponse(com.coreos.jetcd.api.MemberAddResponse response) {
    super(response, response.getHeader());
    member = new Member(response.getMember());
  }

  /**
   * returns the member information for the added member.
   */
  public Member getMember() {
    return member;
  }

  /**
   * returns a list of all members after adding the new member.
   */
  public synchronized List<Member> getMembers() {
    if (members == null) {
      members = Util.toMembers(getResponse().getMembersList());
    }

    return members;
  }
}
