package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * AuthRoleGetResponse returned by {@link Auth#roleGet(ByteSequence)} contains
 * a header and a list of permissions.
 */
public class AuthRoleGetResponse {

  private final Header header;
  private final List<Permission> permissions;

  public AuthRoleGetResponse(Header header, List<Permission> permissions) {
    this.header = header;
    this.permissions = permissions;
  }

  public Header getHeader() {
    return header;
  }

  public List<Permission> getPermissions() {
    return permissions;
  }
}
