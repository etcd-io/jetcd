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

import com.coreos.jetcd.api.Auth;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;
import java.util.List;

/**
 * AuthUserGetResponse returned by {@link Auth#userGet(ByteSequence)} contains a header and
 * a list of roles associated with the user.
 */
public class AuthUserGetResponse extends
    AbstractResponse<com.coreos.jetcd.api.AuthUserGetResponse> {

  public AuthUserGetResponse(com.coreos.jetcd.api.AuthUserGetResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns a list of roles.
   */
  public List<String> getRoles() {
    return getResponse().getRolesList();
  }
}
