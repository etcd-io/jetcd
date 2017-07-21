package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;

/**
 * AuthEnableResponse returned by {@link Auth#authEnable()} call contains a header.
 */
public class AuthEnableResponse extends AbstractResponse<com.coreos.jetcd.api.AuthEnableResponse> {

  public AuthEnableResponse(com.coreos.jetcd.api.AuthEnableResponse response) {
    super(response, response.getHeader());
  }
}
