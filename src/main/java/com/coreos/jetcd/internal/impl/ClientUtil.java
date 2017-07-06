package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Constants;
import com.coreos.jetcd.resolver.SimpleEtcdNameResolverFactory;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.NameResolver;
import io.grpc.stub.AbstractStub;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.UrlValidator;

public final class ClientUtil {

  private ClientUtil() {
  }

  /**
   * add token to channel's head.
   *
   * @param stub the stub to attach head
   * @param token the token for auth
   * @param <T> the type of stub
   * @return the attached stub
   */
  static final <T extends AbstractStub<T>> T configureStub(T stub, Optional<String> token) {
    return token.map(t -> {
          Metadata metadata = new Metadata();
          Metadata.Key<String> tokenKey = Metadata.Key
              .of(Constants.TOKEN, Metadata.ASCII_STRING_MARSHALLER);
          metadata.put(tokenKey, t);
          CallCredentials callCredentials = (methodDescriptor, attributes,
              executor, metadataApplier) -> metadataApplier.apply(metadata);
          return stub.withCallCredentials(callCredentials);
        }
    ).orElse(stub);
  }

  static final NameResolver.Factory simpleNameResolveFactory(List<String> endpoints) {
    return new SimpleEtcdNameResolverFactory(
        endpoints.stream()
            .map(ClientUtil::endpointToUri)
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

  public static boolean isValidEndpointFormat(String endpoint) {
    if (endpoint.startsWith("http")) {
      return isValidURL(endpoint);
    }

    String[] hostAndPort = endpoint.split(":");

    if (hostAndPort.length != 2) {
      return false;
    }

    String host = hostAndPort[0];
    String port = hostAndPort[1];

    return (isValidHost(host) && isValidPort(port));
  }

  private static boolean isValidURL(String url) {
    String[] scheme = {"http"};
    UrlValidator urlValidator = new UrlValidator(scheme, UrlValidator.ALLOW_LOCAL_URLS);
    return urlValidator.isValid(url);
  }

  private static boolean isValidHost(String host) {
    return InetAddressValidator.getInstance().isValid(host) || "localhost".equals(host);
  }

  private static boolean isValidPort(String port) {
    IntegerValidator portValidator = IntegerValidator.getInstance();
    return portValidator.isValid(port) && portValidator.isInRange(Integer.parseInt(port), 0, 65535);
  }
}
