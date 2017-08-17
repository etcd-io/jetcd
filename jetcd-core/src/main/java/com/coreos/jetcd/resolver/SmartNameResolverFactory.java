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

import io.grpc.Attributes;
import io.grpc.NameResolver;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class SmartNameResolverFactory extends NameResolver.Factory {
  private final List<URI> uris;

  private SmartNameResolverFactory(List<URI> uris) {
    this.uris = uris;
  }

  @Nullable
  @Override
  public NameResolver newNameResolver(URI targetUri, Attributes params) {
    if ("etcd".equals(targetUri.getScheme())) {
      return new SmartNameResolver(this.uris);
    } else {
      return null;
    }
  }

  @Override
  public String getDefaultScheme() {
    return "etcd";
  }


  public static SmartNameResolverFactory forEndpoints(String... endpoints) {
    return forEndpoints(Arrays.asList(endpoints));
  }

  public static SmartNameResolverFactory forEndpoints(Collection<String> endpoints) {
    List<URI> uris = endpoints.stream().map(endpoint -> {
      try {
        return new URI(endpoint);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(e);
      }
    }).collect(Collectors.toList());

    return new SmartNameResolverFactory(uris);
  }
}
