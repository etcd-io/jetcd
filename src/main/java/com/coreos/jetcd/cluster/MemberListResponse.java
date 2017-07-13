package com.coreos.jetcd.cluster;

import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * MemberListResponse returned by {@link Cluster#listMember()}
 * contains a header and a list of members.
 */
public class MemberListResponse {

  private final Header header;
  private final List<Member> members;

  public MemberListResponse(Header header, List<Member> members) {
    this.header = header;
    this.members = members;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * returns a list of members. empty list if none.
   */
  public List<Member> getMembers() {
    return members;
  }
}
