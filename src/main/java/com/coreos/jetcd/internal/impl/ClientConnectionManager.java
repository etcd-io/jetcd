package com.coreos.jetcd.internal.impl;

import static com.coreos.jetcd.internal.impl.ClientUtil.configureStub;
import static com.coreos.jetcd.internal.impl.ClientUtil.defaultChannelBuilder;
import static com.coreos.jetcd.internal.impl.ClientUtil.generateToken;
import static com.coreos.jetcd.internal.impl.ClientUtil.simpleNameResolveFactory;

import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.stub.AbstractStub;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

class ClientConnectionManager {
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
            resolverFactory = simpleNameResolveFactory(builder.getEndpoints());
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
          tk = generateToken(getChannel(), builder.getUser(), builder.getPassword());
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

    ManagedChannel channel = ManagedChannelBuilder.forTarget("etcd")
        .nameResolverFactory(simpleNameResolveFactory(Collections.singletonList(endpoint)))
        .usePlaintext(true)
        .build();

    try {
      Optional<String> token = generateToken(channel, builder.getUser(), builder.getPassword());
      T stub = configureStub(stubCustomizer.apply(channel), token);

      return stubConsumer.apply(stub).whenComplete(
          (r, t) -> channel.shutdown()
      );
    } catch (Exception e) {
      channel.shutdown();
      throw EtcdExceptionFactory.newEtcdException(e);
    }
  }
}
