package com.coreos.jetcd;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.coreos.jetcd.resolver.SimpleEtcdNameResolverFactory;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.NameResolver;
import io.grpc.stub.AbstractStub;

public final class EtcdClientUtil {

  private EtcdClientUtil() {
  }

  /**
   * add token to channel's head
   *
   * @param stub the stub to attach head
   * @param token the token for auth
   * @param <T> the type of stub
   * @return the attached stub
   */
  static final <T extends AbstractStub<T>> T configureStub(T stub, Optional<String> token) {
    return token.map(t -> {
          Metadata metadata = new Metadata();
          Metadata.Key<String> TOKEN_KEY = Metadata.Key
              .of(EtcdConstants.TOKEN, Metadata.ASCII_STRING_MARSHALLER);
          metadata.put(TOKEN_KEY, t);
          CallCredentials callCredentials = (methodDescriptor, attributes, executor, metadataApplier) -> metadataApplier
              .apply(metadata);
          return stub.withCallCredentials(callCredentials);
        }
    ).orElse(stub);
  }

  static final NameResolver.Factory simpleNameResolveFactory(List<String> endpoints) {
    return new SimpleEtcdNameResolverFactory(
        endpoints.stream()
            .map(EtcdClientUtil::endpointToUri)
            .collect(Collectors.toList())
    );
  }

  static URI endpointToUri(String endpoint) {
    try {
      if (!endpoint.startsWith("http://")) {
        endpoint = "http://" + endpoint;
      }
      return new URI(endpoint);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  static ManagedChannelBuilder<?> defaultChannelBuilder(NameResolver.Factory factory) {
    return ManagedChannelBuilder.forTarget("etcd")
        .nameResolverFactory(factory)
        .usePlaintext(true);
  }
}
