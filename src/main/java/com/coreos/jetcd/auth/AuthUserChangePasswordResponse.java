package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthUserChangePasswordResponse returned by {@link Auth#userChangePassword(ByteSequence,
 * ByteSequence)} contains a header.
 */
public class AuthUserChangePasswordResponse {

  private final Header header;

  public AuthUserChangePasswordResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
