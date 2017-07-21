package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;
import java.util.List;

/**
 * AuthUserGetResponse returned by {@link Auth#userGet(ByteSequence)} contains a header and
 * a list of roles associated with the user.
 */
public class AuthUserGetResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserGetResponse> {

  public AuthUserGetResponse(com.coreos.jetcd.api.AuthUserGetResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns a list of roles.
   */
  public List<String> getRoles() {
    return getResponse().getRolesList();
  }
}
