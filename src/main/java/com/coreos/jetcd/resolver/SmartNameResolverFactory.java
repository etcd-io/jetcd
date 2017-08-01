package com.coreos.jetcd.resolver;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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

  public static SmartNameResolverFactory forEndpoints(List<String> endpoints) {
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
