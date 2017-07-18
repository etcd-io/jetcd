package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthUserRevokeRoleResponse returned by {@link Auth#userRevokeRole(ByteSequence, ByteSequence)}
 * contains a header.
 */
public class AuthUserRevokeRoleResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserRevokeRoleResponse> {

  public AuthUserRevokeRoleResponse(com.coreos.jetcd.api.AuthUserRevokeRoleResponse response) {
    super(response, response.getHeader());
  }
}
