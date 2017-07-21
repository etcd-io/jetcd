package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthUserGrantRoleResponse returned by {@link Auth#userGrantRole(ByteSequence,
 * ByteSequence)} contains a header.
 */
public class AuthUserGrantRoleResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserGrantRoleResponse> {

  public AuthUserGrantRoleResponse(com.coreos.jetcd.api.AuthUserGrantRoleResponse response) {
    super(response, response.getHeader());
  }
}
