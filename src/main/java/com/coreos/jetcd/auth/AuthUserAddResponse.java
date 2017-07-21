package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthUserAddResponse returned by {@link Auth#userAdd(ByteSequence, ByteSequence)} contains a
 * header.
 */
public class AuthUserAddResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserAddResponse> {

  public AuthUserAddResponse(com.coreos.jetcd.api.AuthUserAddResponse response) {
    super(response, response.getHeader());
  }
}
