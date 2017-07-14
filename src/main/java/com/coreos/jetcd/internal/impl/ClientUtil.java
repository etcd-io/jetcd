package com.coreos.jetcd.internal.impl;

import static com.coreos.jetcd.exception.EtcdExceptionFactory.newAuthFailedException;
import static com.coreos.jetcd.exception.EtcdExceptionFactory.newConnectException;
import static com.coreos.jetcd.internal.impl.Util.byteStringFromByteSequence;
import static com.google.common.base.Preconditions.checkArgument;

import com.coreos.jetcd.Constants;
import com.coreos.jetcd.api.AuthGrpc;
import com.coreos.jetcd.api.AuthenticateRequest;
import com.coreos.jetcd.api.AuthenticateResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.coreos.jetcd.resolver.SimpleNameResolverFactory;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.NameResolver;
import io.grpc.stub.AbstractStub;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

final class ClientUtil {

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
          metadata.put(Metadata.Key.of(Constants.TOKEN, Metadata.ASCII_STRING_MARSHALLER), t);

          return stub.withCallCredentials(
              (methodDescriptor, attributes, executor, applier) -> applier.apply(metadata)
          );
        }
    ).orElse(stub);
  }

  static final NameResolver.Factory simpleNameResolveFactory(List<String> endpoints) {
    return new SimpleNameResolverFactory(
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

  /**
   * get token from etcd with name and password.
   *
   * @param channel channel to etcd
   * @param username auth name
   * @param password auth password
   * @return authResp
   */
  static ListenableFuture<AuthenticateResponse> authenticate(
      ManagedChannel channel, ByteSequence username, ByteSequence password) {

    ByteString user = byteStringFromByteSequence(username);
    ByteString pass = byteStringFromByteSequence(password);

    checkArgument(!user.isEmpty(), "username can not be empty.");
    checkArgument(!pass.isEmpty(), "password can not be empty.");

    return AuthGrpc.newFutureStub(channel).authenticate(
        AuthenticateRequest.newBuilder()
            .setNameBytes(user)
            .setPasswordBytes(pass)
            .build()
    );
  }

  /**
   * get token with ClientBuilder.
   *
   * @param username auth user name
   * @param password auth user password
   * @return the auth token
   * @throws ConnectException This may be caused as network reason, wrong address
   * @throws AuthFailedException This may be caused as wrong username or password
   */
  static Optional<String> generateToken(
      ManagedChannel channel, ByteSequence username, ByteSequence password)
        throws ConnectException, AuthFailedException {

    if (username != null && password != null) {
      try {
        return Optional.of(
            ClientUtil.authenticate(channel, username, password).get().getToken()
        );
      } catch (InterruptedException ite) {
        throw newConnectException("connect to etcd failed", ite);
      } catch (ExecutionException exee) {
        throw newAuthFailedException("auth failed as wrong username or password", exee);
      }
    }
    return Optional.empty();
  }
}
