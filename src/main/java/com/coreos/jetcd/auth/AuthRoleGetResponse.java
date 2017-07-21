package com.coreos.jetcd.auth;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.auth.Permission.Type;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AuthRoleGetResponse returned by {@link Auth#roleGet(ByteSequence)} contains
 * a header and a list of permissions.
 */
public class AuthRoleGetResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthRoleGetResponse> {

  private List<Permission> permissions;

  public AuthRoleGetResponse(com.coreos.jetcd.api.AuthRoleGetResponse response) {
    super(response, response.getHeader());
  }

  private static Permission toPermission(com.coreos.jetcd.api.Permission perm) {
    ByteSequence key = ByteSequence.fromBytes(perm.getKey().toByteArray());
    ByteSequence rangeEnd = ByteSequence.fromBytes(perm.getRangeEnd().toByteArray());
    Permission.Type type;
    switch (perm.getPermType()) {
      case READ:
        type = Type.READ;
        break;
      case WRITE:
        type = Type.WRITE;
        break;
      case READWRITE:
        type = Type.READWRITE;
        break;
      default:
        type = Type.UNRECOGNIZED;
    }
    return new Permission(type, key, rangeEnd);
  }

  public synchronized List<Permission> getPermissions() {
    if (permissions == null) {
      permissions = getResponse().getPermList().stream()
          .map(AuthRoleGetResponse::toPermission)
          .collect(Collectors.toList());
    }

    return permissions;
  }
}
