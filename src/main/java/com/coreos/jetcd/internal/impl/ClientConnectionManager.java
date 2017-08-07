package com.coreos.jetcd.internal.impl;

import static com.coreos.jetcd.exception.EtcdExceptionFactory.newAuthFailedException;
import static com.coreos.jetcd.exception.EtcdExceptionFactory.newConnectException;
import static com.coreos.jetcd.internal.impl.Util.byteStringFromByteSequence;
import static com.coreos.jetcd.resolver.SmartNameResolverFactory.forEndpoints;
import static com.google.common.base.Preconditions.checkArgument;

import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.api.AuthGrpc;
import com.coreos.jetcd.api.AuthenticateRequest;
import com.coreos.jetcd.api.AuthenticateResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
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
import io.grpc.Status.Code;
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
  private static final Metadata.Key<String> TOKEN = Metadata.Key
      .of("token", Metadata.ASCII_STRING_MARSHALLER);

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
    ManagedChannel managedChannel = channelRef.get();
    if (managedChannel == null) {
      synchronized (channelRef) {
        managedChannel = channelRef.get();
        if (managedChannel == null) {
          ManagedChannelBuilder<?> channelBuilder = builder.getChannelBuilder();

          // If channel builder is provided, any additional configuration such as SslContext, name
          // resolver or load balancer is ignored
          if (channelBuilder == null) {
            channelBuilder = defaultChannelBuilder();
          }

          managedChannel = channelBuilder.build();
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
      channel.shutdown();
    }

    executorService.shutdownNow();
  }

  <T extends AbstractStub<T>, R> CompletableFuture<R> withNewChannel(
      String endpoint,
      Function<ManagedChannel, T> stubCustomizer,
      Function<T, CompletableFuture<R>> stubConsumer) {

    ManagedChannelBuilder<?> channelBuilder = defaultChannelBuilder();
    channelBuilder.nameResolverFactory(forEndpoints(endpoint));

    ManagedChannel channel = channelBuilder.build();

    try {
      T stub = stubCustomizer.apply(channel);

      return stubConsumer.apply(stub).whenComplete(
          (r, t) -> channel.shutdown()
      );
    } catch (Exception e) {
      channel.shutdown();
      throw EtcdExceptionFactory.newEtcdException(e);
    }
  }

  private ManagedChannelBuilder<?> defaultChannelBuilder() {
    NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget("etcd");

    if (builder.getSslContext() != null) {
      channelBuilder.sslContext(builder.getSslContext());
    } else {
      channelBuilder.usePlaintext(true);
    }

    channelBuilder.nameResolverFactory(forEndpoints(builder.getEndpoints()));

    if (builder.getLoadBalancerFactory() != null) {
      channelBuilder.loadBalancerFactory(builder.getLoadBalancerFactory());
    }

    channelBuilder.intercept(new AuthTokenInterceptor());

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
      Channel channel, ByteSequence username, ByteSequence password) {

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
  private Optional<String> generateToken(Channel channel)
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
              if (status.getCode() == Code.UNAUTHENTICATED
                  && "etcdserver: invalid auth token".equals(status.getDescription())) {
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
