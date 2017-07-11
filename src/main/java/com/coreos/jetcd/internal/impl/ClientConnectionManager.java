package com.coreos.jetcd.internal.impl;

import static com.coreos.jetcd.exception.EtcdExceptionFactory.newAuthFailedException;
import static com.coreos.jetcd.exception.EtcdExceptionFactory.newConnectException;
import static com.coreos.jetcd.internal.impl.Util.byteStringFromByteSequence;
import static com.google.common.base.Preconditions.checkArgument;

import com.coreos.jetcd.ClientBuilder;
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
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

final class ClientConnectionManager {
  private final ClientBuilder builder;
  private final AtomicReference<ManagedChannel> channelRef;
  private final AtomicReference<Optional<String>> tokenRef;
  private final ExecutorService executorService;

  ClientConnectionManager(ClientBuilder builder) {
    this(builder, Executors.newCachedThreadPool(), null);
  }

  ClientConnectionManager(
      ClientBuilder builder, ExecutorService executorService) {

    this(builder, executorService, null);
  }

  ClientConnectionManager(
      ClientBuilder builder, ManagedChannel channel) {

    this(builder, Executors.newCachedThreadPool(), channel);
  }

  ClientConnectionManager(
      ClientBuilder builder, ExecutorService executorService, ManagedChannel channel) {

    this.builder = builder;
    this.channelRef = new AtomicReference<>(channel);
    this.tokenRef = new AtomicReference<>();
    this.executorService = executorService;
  }

  ManagedChannel getChannel() {
    ManagedChannel mc = channelRef.get();
    if (mc == null) {
      synchronized (channelRef) {
        mc = channelRef.get();
        if (mc == null) {
          NameResolver.Factory resolverFactory = builder.getNameResolverFactory();
          if (resolverFactory == null) {
            resolverFactory = SimpleNameResolverFactory.forEndpoints(builder.getEndpoints());
          }

          ManagedChannelBuilder<?> channelBuilder = builder.getChannelBuilder();
          if (channelBuilder == null) {
            channelBuilder = defaultChannelBuilder(resolverFactory);
          }

          mc = channelBuilder.build();
          channelRef.lazySet(mc);
        }
      }
    }

    return mc;
  }

  Optional<String> getToken() {
    Optional<String> tk = tokenRef.get();
    if (tk == null) {
      synchronized (tokenRef) {
        tk = tokenRef.get();
        if (tk == null) {
          tk = generateToken(getChannel());
          tokenRef.lazySet(tk);
        }
      }
    }

    return tk;
  }

  ExecutorService getExecutorService() {
    return executorService;
  }

  /**
   * create and add token to channel's head.
   *
   * @param supplier the stub supplier
   * @param <T> the type of stub
   * @return the attached stub
   */
  <T extends AbstractStub<T>> T newStub(Function<ManagedChannel, T> supplier) {
    return configureStub(supplier.apply(getChannel()), getToken());
  }

  synchronized void close() {
    ManagedChannel channel = channelRef.get();
    if (channel != null) {
      channel.shutdown();
    }

    executorService.shutdownNow();
  }

  <T extends AbstractStub<T>, R> CompletableFuture<R> withNewChannel(
      String endpoint,
      Function<ManagedChannel, T> stubCustomizer,
      Function<T, CompletableFuture<R>> stubConsumer) {

    NameResolver.Factory resolverFactory = SimpleNameResolverFactory.forEndpoints(endpoint);
    ManagedChannel channel = defaultChannelBuilder(resolverFactory).build();

    try {
      Optional<String> token = generateToken(channel);
      T stub = configureStub(stubCustomizer.apply(channel), token);

      return stubConsumer.apply(stub).whenComplete(
          (r, t) -> channel.shutdown()
      );
    } catch (Exception e) {
      channel.shutdown();
      throw EtcdExceptionFactory.newEtcdException(e);
    }
  }

  private ManagedChannelBuilder<?> defaultChannelBuilder(NameResolver.Factory factory) {
    NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget("etcd");
    channelBuilder.nameResolverFactory(factory);

    if (builder.getSslContext() != null) {
      channelBuilder.sslContext(builder.getSslContext());
    } else {
      channelBuilder.usePlaintext(true);
    }

    return channelBuilder;
  }

  /**
   * get token from etcd with name and password.
   *
   * @param channel channel to etcd
   * @param username auth name
   * @param password auth password
   * @return authResp
   */
  private ListenableFuture<AuthenticateResponse> authenticate(
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
   * @return the auth token
   * @throws ConnectException This may be caused as network reason, wrong address
   * @throws AuthFailedException This may be caused as wrong username or password
   */
  private Optional<String> generateToken(ManagedChannel channel)
      throws ConnectException, AuthFailedException {

    if (builder.getUser() != null && builder.getPassword() != null) {
      try {
        return Optional.of(
            authenticate(channel, builder.getUser(), builder.getPassword()).get().getToken()
        );
      } catch (InterruptedException ite) {
        throw newConnectException("connect to etcd failed", ite);
      } catch (ExecutionException exee) {
        throw newAuthFailedException("auth failed as wrong username or password", exee);
      }
    }
    return Optional.empty();
  }

  /**
   * add token to channel's head.
   *
   * @param stub the stub to attach head
   * @param token the token for auth
   * @param <T> the type of stub
   * @return the attached stub
   */
  private <T extends AbstractStub<T>> T configureStub(T stub, Optional<String> token) {
    return token.map(t -> {
          Metadata metadata = new Metadata();
          metadata.put(Metadata.Key.of(Constants.TOKEN, Metadata.ASCII_STRING_MARSHALLER), t);

          return stub.withCallCredentials(
              (methodDescriptor, attributes, executor, applier) -> applier.apply(metadata)
          );
        }
    ).orElse(stub);
  }
}
