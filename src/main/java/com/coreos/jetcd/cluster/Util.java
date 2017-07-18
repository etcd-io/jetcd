package com.coreos.jetcd.cluster;

import com.coreos.jetcd.api.Member;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Util class for Cluster models.
 */
public class Util {

  // toMembers converts a list of API member to a List of client side member.
  static List<com.coreos.jetcd.cluster.Member> toMembers(List<Member> members) {
    return members.stream()
        .map(com.coreos.jetcd.cluster.Member::new)
        .collect(Collectors.toList());
  }
}
