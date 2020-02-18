/*
 * Copyright 2016-2020 The jetcd authors
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

import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.api.Auth;

/**
 * AuthRoleListResponse returned by {@link Auth#roleList()} contains a header and
 * a list of roles.
 */
public class AuthRoleListResponse extends AbstractResponse<io.etcd.jetcd.api.AuthRoleListResponse> {

    public AuthRoleListResponse(io.etcd.jetcd.api.AuthRoleListResponse response) {
        super(response, response.getHeader());
    }

    /**
     * returns a list of roles.
     */
    public List<String> getRoles() {
        return getResponse().getRolesList();
    }
}
