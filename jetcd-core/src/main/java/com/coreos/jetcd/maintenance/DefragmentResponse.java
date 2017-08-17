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

package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.data.AbstractResponse;

/**
 * DefragmentResponse returned by {@link Maintenance#defragmentMember(String)} contains a header.
 */
public class DefragmentResponse extends AbstractResponse<com.coreos.jetcd.api.DefragmentResponse> {

  public DefragmentResponse(com.coreos.jetcd.api.DefragmentResponse response) {
    super(response, response.getHeader());
  }
}
