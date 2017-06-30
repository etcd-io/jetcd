package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * AuthRoleListResponse returned by {@link Auth#roleList()} contains a header and
 * a list of roles.
 */
public class AuthRoleListResponse {

  private final Header header;
  private final List<String> roles;

  public AuthRoleListResponse(Header header, List<String> roles) {
    this.header = header;
    this.roles = roles;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * returns a list of roles.
   */
  public List<String> getRoles() {
    return roles;
  }
}
