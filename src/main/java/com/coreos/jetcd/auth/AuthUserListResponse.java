package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;

/**
 * AuthUserListResponse returned by {@link Auth#userList()} contains a header and
 * a list of users.
 */
public class AuthUserListResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserListResponse> {

  public AuthUserListResponse(com.coreos.jetcd.api.AuthUserListResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns a list of users.
   */
  public List<String> getUsers() {
    return getResponse().getUsersList();
  }
}
