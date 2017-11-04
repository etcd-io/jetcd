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

package com.coreos.jetcd.resolver;

import com.coreos.jetcd.exception.ErrorCode;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.google.common.base.Strings;
import io.grpc.EquivalentAddressGroup;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DirectUriResolver implements URIResolver {

  private static final List<String> SCHEMES = Arrays.asList("http", "https");

  private ConcurrentMap<URI, List<EquivalentAddressGroup>> cache;

  DirectUriResolver() {
    this.cache = new ConcurrentHashMap<>();
  }

  @Override
  public int priority() {
    return Integer.MIN_VALUE;
  }

  @Override
  public boolean supports(URI uri) {
    if (!SCHEMES.contains(uri.getScheme())) {
      return false;
    }

    if (!Strings.isNullOrEmpty(uri.getPath())) {
      return false;
    }

    if (uri.getPort() == -1) {
      return false;
    }

    return true;
  }

  @Override
  public List<EquivalentAddressGroup> resolve(URI uri) {
    if (!supports(uri)) {
      // Wrap as etcd exception but set a proper cause
      throw EtcdExceptionFactory.newEtcdException(
          ErrorCode.INVALID_ARGUMENT,
          "Unsupported URI " + uri
      );
    }

    return this.cache.computeIfAbsent(
        uri,
        u -> Collections.singletonList(
            new EquivalentAddressGroup(new InetSocketAddress(uri.getHost(), uri.getPort()))
        )
    );
  }
}
