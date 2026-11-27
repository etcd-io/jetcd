/*
 * Copyright 2016-2021 The jetcd authors
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

package io.etcd.jetcd.auth;

import java.util.List;
import java.util.function.Supplier;

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.common.suppliers.Suppliers;
import io.etcd.jetcd.impl.AbstractResponse;

/**
 * AuthRoleGetResponse returned by {@link Auth#roleGet(ByteSequence)} contains
 * a header and a list of permissions.
 */
public class AuthRoleGetResponse extends AbstractResponse<io.etcd.jetcd.api.AuthRoleGetResponse> {

    private final Supplier<List<Permission>> permissions;

    /**
     * Creates a new AuthRoleGetResponse from the gRPC response.
     *
     * @param response the gRPC auth role get response
     */
    public AuthRoleGetResponse(io.etcd.jetcd.api.AuthRoleGetResponse response) {
        super(response, response.getHeader());

        this.permissions = Suppliers.memoizing(
            () -> getResponse().getPermList().stream().map(AuthRoleGetResponse::toPermission).toList());
    }

    private static Permission toPermission(io.etcd.jetcd.api.Permission perm) {
        ByteSequence key = ByteSequence.from(perm.getKey());
        ByteSequence rangeEnd = ByteSequence.from(perm.getRangeEnd());

        Permission.Type type = switch (perm.getPermType()) {
            case READ -> Permission.Type.READ;
            case WRITE -> Permission.Type.WRITE;
            case READWRITE -> Permission.Type.READWRITE;
            default -> Permission.Type.UNRECOGNIZED;
        };

        return new Permission(type, key, rangeEnd);
    }

    /**
     * Returns the list of permissions for this role.
     *
     * @return the list of permissions
     */
    public List<Permission> getPermissions() {
        return permissions.get();
    }
}
