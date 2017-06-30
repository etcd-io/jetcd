package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.api.Permission.Type;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;

/**
 * AuthRoleGrantPermissionResponse returned by {@link Auth#roleGrantPermission(ByteSequence,
 * ByteSequence, ByteSequence, Type)} contains a header.
 */
public class AuthRoleGrantPermissionResponse {

  private final Header header;

  public AuthRoleGrantPermissionResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
