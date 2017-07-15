package com.coreos.jetcd.cluster;

import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * MemberAddResponse returned by {@link com.coreos.jetcd.Cluster#addMember(List)}
 * contains a header, added member, and list of members after adding the new member.
 */
public class MemberAddResponse {

  private Header header;
  private Member member;
  private List<Member> members;

  public MemberAddResponse(Header header, Member member, List<Member> members) {
    this.header = header;
    this.member = member;
    this.members = members;
  }

  public Header getHeader() {
    return header;
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
  public List<Member> getMembers() {
    return members;
  }

}
