package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.Header;

/**
 * AuthEnableResponse returned by {@link Auth#authEnable()} call contains a header.
 */
public class AuthEnableResponse {

  private final Header header;

  public AuthEnableResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
