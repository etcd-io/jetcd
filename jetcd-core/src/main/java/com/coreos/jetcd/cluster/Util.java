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

package com.coreos.jetcd.cluster;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Util class for Cluster models.
 */
public class Util {

  /**
   * Converts a list of API member to a List of client side member.
   */
  static List<com.coreos.jetcd.cluster.Member> toMembers(
      List<com.coreos.jetcd.api.Member> members) {

    return members.stream()
        .map(com.coreos.jetcd.cluster.Member::new)
        .collect(Collectors.toList());
  }
}
