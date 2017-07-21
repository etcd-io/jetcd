package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.api.Permission.Type;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthRoleGrantPermissionResponse returned by {@link Auth#roleGrantPermission(ByteSequence,
 * ByteSequence, ByteSequence, Type)} contains a header.
 */
public class AuthRoleGrantPermissionResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthRoleGrantPermissionResponse> {

  public AuthRoleGrantPermissionResponse(
      com.coreos.jetcd.api.AuthRoleGrantPermissionResponse response) {
    super(response, response.getHeader());
  }
}
