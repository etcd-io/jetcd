package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthUserDeleteResponse returned by {@link Auth#userDelete(ByteSequence)} contains a header.
 */
public class AuthUserDeleteResponse {

  private final Header header;

  public AuthUserDeleteResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
