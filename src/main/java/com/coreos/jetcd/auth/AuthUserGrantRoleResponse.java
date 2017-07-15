package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthUserGrantRoleResponse returned by {@link Auth#userGrantRole(ByteSequence,
 * ByteSequence)} contains a header.
 */
public class AuthUserGrantRoleResponse {

  private final Header header;

  public AuthUserGrantRoleResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
