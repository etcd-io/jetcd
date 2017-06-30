package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * AuthUserGetResponse returned by {@link Auth#userGet(ByteSequence)} contains a header and
 * a list of roles associated with the user.
 */
public class AuthUserGetResponse {

  private final Header header;

  private final List<String> roles;

  public AuthUserGetResponse(Header header, List<String> roles) {
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
