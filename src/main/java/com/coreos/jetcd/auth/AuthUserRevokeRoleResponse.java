package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthUserRevokeRoleResponse returned by {@link Auth#userRevokeRole(ByteSequence, ByteSequence)}
 * contains a header.
 */
public class AuthUserRevokeRoleResponse {

  private final Header header;

  public AuthUserRevokeRoleResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
