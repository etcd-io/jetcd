package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;

/**
 * AuthRoleRevokePermissionResponse returned by {@link Auth#roleRevokePermission(ByteSequence,
 * ByteSequence, ByteSequence)} contains a header.
 */
public class AuthRoleRevokePermissionResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthRoleRevokePermissionResponse> {

  public AuthRoleRevokePermissionResponse(
      com.coreos.jetcd.api.AuthRoleRevokePermissionResponse response) {
    super(response, response.getHeader());
  }
}
