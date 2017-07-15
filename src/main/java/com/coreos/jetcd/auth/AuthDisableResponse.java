package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.Header;

/**
 * AuthDisableResponse returned by {@link Auth#authDisable()} contains a header.
 */
public class AuthDisableResponse {

  private final Header header;

  public AuthDisableResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
