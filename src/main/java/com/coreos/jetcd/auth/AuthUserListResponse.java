package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * AuthUserListResponse returned by {@link Auth#userList()} contains a header and
 * a list of users.
 */
public class AuthUserListResponse {

  private final Header header;
  private final List<String> users;

  public AuthUserListResponse(Header header, List<String> users) {
    this.header = header;
    this.users = users;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * returns a list of users.
   */
  public List<String> getUsers() {
    return users;
  }
}
