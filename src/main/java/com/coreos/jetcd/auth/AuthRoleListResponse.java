package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;

/**
 * AuthRoleListResponse returned by {@link Auth#roleList()} contains a header and
 * a list of roles.
 */
public class AuthRoleListResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthRoleListResponse> {

  public AuthRoleListResponse(com.coreos.jetcd.api.AuthRoleListResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns a list of roles.
   */
  public List<String> getRoles() {
    return getResponse().getRolesList();
  }
}
