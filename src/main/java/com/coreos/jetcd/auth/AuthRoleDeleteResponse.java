package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthRoleDeleteResponse returned by {@link Auth#roleDelete(ByteSequence)}
 * contains a header.
 */
public class AuthRoleDeleteResponse {

  private final Header header;

  public AuthRoleDeleteResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
