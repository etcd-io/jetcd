/*
 * Copyright 2016-2019 The jetcd authors
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

package io.etcd.jetcd;

import static com.google.common.base.Preconditions.checkArgument;
import static io.etcd.jetcd.Util.isInvalidTokenError;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.handleInterrupt;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static io.etcd.jetcd.resolver.SmartNameResolverFactory.forEndpoints;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.etcd.jetcd.api.AuthGrpc;
import io.etcd.jetcd.api.AuthenticateRequest;
import io.etcd.jetcd.api.AuthenticateResponse;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.resolver.URIResolverLoader;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.annotation.Nonnull;

final class ClientConnectionManager {

  private static final Metadata.Key<String> TOKEN = Metadata.Key
      .of("token", Metadata.ASCII_STRING_MARSHALLER);

  private final ClientBuilder builder;
  private final AtomicReference<ManagedChannel> channelRef;
  private final AtomicReference<Optional<String>> tokenRef;
  private final ExecutorService executorService;

  ClientConnectionManager(ClientBuilder builder) {
    this(builder, null);
  }

  ClientConnectionManager(
      ClientBuilder builder, ManagedChannel channel) {

    this.builder = builder;
    this.channelRef = new AtomicReference<>(channel);
    this.tokenRef = new AtomicReference<>();
    if (builder.executorService() == null) {
      this.executorService = Executors.newCachedThreadPool();
    } else {
      this.executorService = builder.executorService();
    }
  }

  ManagedChannel getChannel() {
    ManagedChannel managedChannel = channelRef.get();
    if (managedChannel == null) {
      synchronized (channelRef) {
        managedChannel = channelRef.get();
        if (managedChannel == null) {
          managedChannel = defaultChannelBuilder().build();
          channelRef.lazySet(managedChannel);
        }
      }
    }

    return managedChannel;
  }

  private Optional<String> getToken(Channel channel) {
    Optional<String> tk = tokenRef.get();
    if (tk == null) {
      synchronized (tokenRef) {
        tk = tokenRef.get();
        if (tk == null) {
          tk = generateToken(channel);
          tokenRef.lazySet(tk);
        }
      }
    }

    return tk;
  }

  private void refreshToken(Channel channel) {
    synchronized (tokenRef) {
      Optional<String> tk = generateToken(channel);
      tokenRef.lazySet(tk);
    }
  }

  ByteSequence getNamespace() {
    return builder.namespace();
  }

  ExecutorService getExecutorService() {
    return executorService;
  }

  /**
   * create stub with saved channel.
   *
   * @param supplier the stub supplier
   * @param <T> the type of stub
   * @return the attached stub
   */
  <T extends AbstractStub<T>> T newStub(Function<ManagedChannel, T> supplier) {
    return supplier.apply(getChannel());
  }

  synchronized void close() {
    ManagedChannel channel = channelRef.get();
    if (channel != null) {
      channel.shutdownNow();
    }

    executorService.shutdownNow();
  }

  <T extends AbstractStub<T>, R> CompletableFuture<R> withNewChannel(
      URI endpoint,
      Function<ManagedChannel, T> stubCustomizer,
      Function<T, CompletableFuture<R>> stubConsumer) {

    final ManagedChannel channel = defaultChannelBuilder()
        .nameResolverFactory(
            forEndpoints(
                Util.supplyIfNull(builder.authority(), () -> "etcd"),
                Collections.singleton(endpoint),
                Util.supplyIfNull(builder.uriResolverLoader(), URIResolverLoader::defaultLoader)
            )
        ).build();

    try {
      T stub = stubCustomizer.apply(channel);

      return stubConsumer.apply(stub).whenComplete(
          (r, t) -> channel.shutdown()
      );
    } catch (Exception e) {
      channel.shutdown();
      throw EtcdExceptionFactory.toEtcdException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @VisibleForTesting
  protected ManagedChannelBuilder<?> defaultChannelBuilder() {
    final NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget("etcd");

    if (builder.maxInboundMessageSize() != null) {
      channelBuilder.maxInboundMessageSize(builder.maxInboundMessageSize());
    }
    if (builder.sslContext() != null) {
      channelBuilder.negotiationType(NegotiationType.TLS);
      channelBuilder.sslContext(builder.sslContext());
    } else {
      channelBuilder.negotiationType(NegotiationType.PLAINTEXT);
    }

    channelBuilder.nameResolverFactory(
        forEndpoints(
          Util.supplyIfNull(builder.authority(), () -> "etcd"),
          builder.endpoints(),
          Util.supplyIfNull(builder.uriResolverLoader(), URIResolverLoader::defaultLoader)
        )
    );

    if (builder.loadBalancerFactory() != null) {
      channelBuilder.loadBalancerFactory(builder.loadBalancerFactory());
    } else {
      channelBuilder.loadBalancerFactory(new PickFirstLoadBalancerProvider());
    }

    channelBuilder.intercept(new AuthTokenInterceptor());

    if (builder.headers() != null) {
      channelBuilder.intercept(new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
          return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
              builder.headers().forEach(headers::put);
              super.start(responseListener, headers);
            }
          };
        }
      });
    }

    if (builder.interceptors() != null) {
      channelBuilder.intercept(builder.interceptors());
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
  private static ListenableFuture<AuthenticateResponse> authenticate(
      @Nonnull Channel channel,
      @Nonnull ByteSequence username,
      @Nonnull ByteSequence password) {

    final ByteString user = username.getByteString();
    final ByteString pass = password.getByteString();

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
   * @throws io.etcd.jetcd.common.exception.EtcdException a exception indicates failure reason.
   */
  private Optional<String> generateToken(Channel channel) {

    if (builder.user() != null && builder.password() != null) {
      try {
        return Optional.of(
            authenticate(channel, builder.user(), builder.password()).get().getToken()
        );
      } catch (InterruptedException ite) {
        throw handleInterrupt(ite);
      } catch (ExecutionException exee) {
        throw toEtcdException(exee);
      }
    }
    return Optional.empty();
  }


  /**
   * AuthTokenInterceptor fills header with Auth token of any rpc calls and
   * refreshes token if the rpc results an invalid Auth token error.
   */
  private class AuthTokenInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          getToken(next).ifPresent(t -> headers.put(TOKEN, t));
          super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
            @Override
            public void onClose(Status status, Metadata trailers) {
              if (isInvalidTokenError(status)) {
                try {
                  refreshToken(next);
                } catch (Exception e) {
                  // don't throw any error here.
                  // rpc will retry on expired auth token.
                }
              }
              super.onClose(status, trailers);
            }
          }, headers);
        }
      };
    }
  }
}
