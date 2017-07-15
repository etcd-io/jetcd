package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthRoleAddResponse returned by {@link Auth#roleAdd(ByteSequence)} contains
 * a header.
 */
public class AuthRoleAddResponse {

  private final Header header;

  public AuthRoleAddResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
