package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthUserAddResponse returned by {@link Auth#userAdd(ByteSequence, ByteSequence)} contains a
 * header.
 */
public class AuthUserAddResponse {

  private final Header header;

  public AuthUserAddResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
