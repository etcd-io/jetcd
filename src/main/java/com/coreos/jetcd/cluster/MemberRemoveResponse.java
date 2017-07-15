package com.coreos.jetcd.cluster;

import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * MemberRemoveResponse returned by {@link com.coreos.jetcd.Cluster#removeMember(long)}
 * contains a header and a list of member the removal of the member.
 */
public class MemberRemoveResponse {

  private Header header;
  private List<Member> members;

  public MemberRemoveResponse(Header header, List<Member> members) {
    this.header = header;
    this.members = members;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * returns a list of all members after removing the member.
   */
  public List<Member> getMembers() {
    return members;
  }
}
