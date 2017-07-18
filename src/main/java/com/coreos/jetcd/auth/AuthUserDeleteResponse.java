package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthUserDeleteResponse returned by {@link Auth#userDelete(ByteSequence)} contains a header.
 */
public class AuthUserDeleteResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserDeleteResponse> {

  public AuthUserDeleteResponse(com.coreos.jetcd.api.AuthUserDeleteResponse response) {
    super(response, response.getHeader());
  }
}
