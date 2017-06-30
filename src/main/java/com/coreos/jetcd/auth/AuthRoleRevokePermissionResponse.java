package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthRoleRevokePermissionResponse returned by {@link Auth#roleRevokePermission(ByteSequence,
 * ByteSequence, ByteSequence)} contains a header.
 */
public class AuthRoleRevokePermissionResponse {

  private final Header header;

  public AuthRoleRevokePermissionResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
