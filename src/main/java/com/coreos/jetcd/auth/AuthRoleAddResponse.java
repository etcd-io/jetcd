package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthRoleAddResponse returned by {@link Auth#roleAdd(ByteSequence)} contains
 * a header.
 */
public class AuthRoleAddResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthRoleAddResponse> {

  public AuthRoleAddResponse(com.coreos.jetcd.api.AuthRoleAddResponse response) {
    super(response, response.getHeader());
  }
}
