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

package io.etcd.jetcd;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

public final class Util {

  private Util() {
  }

  public static List<URI> toURIs(List<String> uris) {
    return uris.stream().map(uri -> {
      try {
        return new URI(uri);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Invalid endpoint URI: " + uri, e);
      }
    }).collect(Collectors.toList());
  }

}
