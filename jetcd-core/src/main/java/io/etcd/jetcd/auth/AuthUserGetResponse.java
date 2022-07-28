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

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.impl.AbstractResponse;

/**
 * AuthUserGetResponse returned by {@link Auth#userGet(ByteSequence)} contains a header and
 * a list of roles associated with the user.
 */
public class AuthUserGetResponse extends AbstractResponse<io.etcd.jetcd.api.AuthUserGetResponse> {

    public AuthUserGetResponse(io.etcd.jetcd.api.AuthUserGetResponse response) {
        super(response, response.getHeader());
    }

    /**
     * Returns a list of roles.
     *
     * @return the roles.
     */
    public List<String> getRoles() {
        return getResponse().getRolesList();
    }
}
