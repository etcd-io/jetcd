package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;

/**
 * AuthDisableResponse returned by {@link Auth#authDisable()} contains a header.
 */
public class AuthDisableResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthDisableResponse> {

  public AuthDisableResponse(com.coreos.jetcd.api.AuthDisableResponse response) {
    super(response, response.getHeader());
  }
}
