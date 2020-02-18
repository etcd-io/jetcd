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

package io.etcd.jetcd.resolver;

import io.grpc.EquivalentAddressGroup;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

public interface URIResolver {
  /**
   * Determine the priority for the resolver. As resolvers are loaded using the ServiceLoader the
   * order is not deterministic and it may change depending on the operating system.
   *
   * @return the priority
   */
  default int priority() {
    return Integer.MAX_VALUE;
  }

  /**
   * Check if this resolver supports the given {@link URI}.
   *
   * @param uri the uri
   * @return true if the resolver supports the URI
   */
  boolean supports(URI uri);

  /**
   * Resolve the uri.
   *
   * @param uri the uri
   * @return a list of {@link EquivalentAddressGroup}
   */
  List<SocketAddress> resolve(URI uri);
}
