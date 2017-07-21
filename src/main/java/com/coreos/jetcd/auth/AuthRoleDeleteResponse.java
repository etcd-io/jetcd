package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthRoleDeleteResponse returned by {@link Auth#roleDelete(ByteSequence)}
 * contains a header.
 */
public class AuthRoleDeleteResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthRoleDeleteResponse> {

  public AuthRoleDeleteResponse(com.coreos.jetcd.api.AuthRoleDeleteResponse response) {
    super(response, response.getHeader());
  }
}
