package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthUserChangePasswordResponse returned by {@link Auth#userChangePassword(ByteSequence,
 * ByteSequence)} contains a header.
 */
public class AuthUserChangePasswordResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserChangePasswordResponse> {

  public AuthUserChangePasswordResponse(
      com.coreos.jetcd.api.AuthUserChangePasswordResponse response) {
    super(response, response.getHeader());
  }
}
