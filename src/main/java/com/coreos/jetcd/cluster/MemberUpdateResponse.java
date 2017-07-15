package com.coreos.jetcd.cluster;

import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * MemberUpdateResponse returned by {@link com.coreos.jetcd.Cluster#updateMember(long, List)}
 * contains a header and a list of members after the member update.
 */
public class MemberUpdateResponse {

  private Header header;
  private List<Member> members;

  public MemberUpdateResponse(Header header, List<Member> members) {
    this.header = header;
    this.members = members;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * returns a list of all members after updating the member.
   */
  public List<Member> getMembers() {
    return members;
  }
}
