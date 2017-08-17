/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
