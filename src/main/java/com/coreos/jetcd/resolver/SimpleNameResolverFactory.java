package com.coreos.jetcd.resolver;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A custom name resolver factory which creates etcd name resolver.
 */
public class SimpleNameResolverFactory extends AbstractEtcdNameResolverFactory {

  private static final String SCHEME = "etcd";
  private static final String NAME = "simple";

  private final List<URI> uris;

  public SimpleNameResolverFactory(List<URI> uris) {
    this.uris = uris;
  }

  @Nullable
  @Override
  public NameResolver newNameResolver(URI targetUri, Attributes params) {
    if (SCHEME.equals(targetUri.getScheme())) {
      String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
      Preconditions.checkArgument(targetPath.startsWith("/"),
          "the path component (%s) of the target (%s) must start with '/'",
          targetPath, targetUri);
      String name = targetPath.substring(1);
      return new SimpleNameResolver(name, GrpcUtil.SHARED_CHANNEL_EXECUTOR, this.uris);
    } else {
      return null;
    }
  }

  @Override
  public String getDefaultScheme() {
    return SCHEME;
  }

  @Override
  public String name() {
    return NAME;
  }

  public static SimpleNameResolverFactory forEndpoints(String... endpoints) {
    return forEndpoints(Arrays.asList(endpoints));
  }

  public static SimpleNameResolverFactory forEndpoints(List<String> endpoints) {
    List<URI> uris = endpoints.stream().map(endpoint -> {
      try {
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
          endpoint = "http://" + endpoint;
        }
        return new URI(endpoint);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(e);
      }
    }).collect(Collectors.toList());

    return new SimpleNameResolverFactory(uris);
  }
}
