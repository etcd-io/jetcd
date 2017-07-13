package com.coreos.jetcd.cluster;

import java.util.List;

public class Member {

  private final long id;

  private final String name;
  private final List<String> peerURLs;
  private final List<String> clientURLS;

  public Member(long id, String name, List<String> peerURLs, List<String> clientURLS) {
    this.id = id;
    this.name = name;
    this.peerURLs = peerURLs;
    this.clientURLS = clientURLS;
  }

  /**
   * returns the member ID for this member.
   */
  public long getId() {
    return id;
  }

  /**
   * returns the human-readable name of the member.
   *
   * <p>If the member is not started, the name will be an empty string.
   */
  public String getName() {
    return name;
  }

  /**
   * returns the list of URLs the member exposes to the cluster for communication.
   */
  public List<String> getPeerURLs() {
    return peerURLs;
  }

  /**
   * returns list of URLs the member exposes to clients for communication.
   *
   * <p>f the member is not started, clientURLs will be empty.
   */
  public List<String> getClientURLS() {
    return clientURLS;
  }
}
