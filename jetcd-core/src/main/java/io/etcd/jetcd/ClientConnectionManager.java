/*
 * Copyright 2016-2021 The jetcd authors
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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.etcd.jetcd.api.AuthGrpc;
import io.etcd.jetcd.api.AuthenticateRequest;
import io.etcd.jetcd.api.AuthenticateResponse;
import io.etcd.jetcd.resolver.DnsSrvNameResolver;
import io.etcd.jetcd.resolver.IPNameResolver;
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
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;
import io.netty.channel.ChannelOption;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static io.etcd.jetcd.Util.isInvalidTokenError;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.handleInterrupt;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

final class ClientConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionManager.class);
    private static final Metadata.Key<String> TOKEN = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);

    private final Object lock;
    private final ClientBuilder builder;
    private final ExecutorService executorService;
    private volatile ManagedChannel managedChannel;
    private volatile String token;

    ClientConnectionManager(ClientBuilder builder) {
        this(builder, null);
    }

    ClientConnectionManager(ClientBuilder builder, ManagedChannel managedChannel) {
        this.lock = new Object();
        this.builder = builder;
        this.managedChannel = managedChannel;

        if (builder.executorService() == null) {
            this.executorService = Executors.newCachedThreadPool();
        } else {
            this.executorService = builder.executorService();
        }
    }

    /**
     * get token from etcd with name and password.
     *
     * @param  channel  channel to etcd
     * @param  username auth name
     * @param  password auth password
     * @return          authResp
     */
    private static ListenableFuture<AuthenticateResponse> authenticate(@Nonnull Channel channel, @Nonnull ByteSequence username,
        @Nonnull ByteSequence password) {

        final ByteString user = username.getByteString();
        final ByteString pass = password.getByteString();

        checkArgument(!user.isEmpty(), "username can not be empty.");
        checkArgument(!pass.isEmpty(), "password can not be empty.");

        return AuthGrpc.newFutureStub(channel)
            .authenticate(AuthenticateRequest.newBuilder().setNameBytes(user).setPasswordBytes(pass).build());
    }

    ManagedChannel getChannel() {
        if (managedChannel == null) {
            synchronized (lock) {
                if (managedChannel == null) {
                    managedChannel = defaultChannelBuilder().build();
                }
            }
        }

        return managedChannel;
    }

    @Nullable
    private String getToken(Channel channel) {
        if (token == null) {
            synchronized (lock) {
                if (token == null) {
                    token = generateToken(channel);
                }
            }
        }

        return token;
    }

    void forceTokenRefresh() {
        synchronized (lock) {
            token = null;
        }
    }

    private void refreshToken(Channel channel) {
        synchronized (lock) {
            token = generateToken(channel);
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
     * @param  supplier the stub supplier
     * @param  <T>      the type of stub
     * @return          the attached stub
     */
    <T extends AbstractStub<T>> T newStub(Function<ManagedChannel, T> supplier) {
        return supplier.apply(getChannel()).withWaitForReady();
    }

    void close() {
        synchronized (lock) {
            if (managedChannel != null) {
                managedChannel.shutdownNow();
            }
        }

        if (builder.executorService() == null) {
            executorService.shutdownNow();
        }
    }

    <T extends AbstractStub<T>, R> CompletableFuture<R> withNewChannel(URI endpoint, Function<ManagedChannel, T> stubCustomizer,
        Function<T, CompletableFuture<R>> stubConsumer) {

        final ManagedChannel channel = defaultChannelBuilder(Collections.singletonList(endpoint)).build();
        final T stub = stubCustomizer.apply(channel).withWaitForReady();

        try {
            return stubConsumer.apply(stub).whenComplete((r, t) -> channel.shutdown());
        } catch (Exception e) {
            channel.shutdown();
            throw toEtcdException(e);
        }
    }

    @VisibleForTesting
    protected ManagedChannelBuilder<?> defaultChannelBuilder() {
        return defaultChannelBuilder(builder.endpoints());
    }

    @VisibleForTesting
    protected ManagedChannelBuilder<?> defaultChannelBuilder(Collection<URI> endpoints) {
        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException("At least one endpoint should be provided");
        }

        final String target;

        if (builder.discovery()) {
            if (endpoints.size() != 1) {
                throw new IllegalArgumentException("When configured for discovery, there should be only a single endpoint");
            }

            target = String.format(
                "%s:///%s",
                DnsSrvNameResolver.SCHEME,
                Iterables.get(endpoints, 0));
        } else {
            target = String.format(
                "%s://%s/%s",
                IPNameResolver.SCHEME,
                builder.authority() != null ? builder.authority() : "",
                endpoints.stream().map(e -> e.getHost() + ":" + e.getPort()).collect(Collectors.joining(",")));
        }

        final NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget(target);

        if (builder.authority() != null) {
            channelBuilder.overrideAuthority(builder.authority());
        }
        if (builder.maxInboundMessageSize() != null) {
            channelBuilder.maxInboundMessageSize(builder.maxInboundMessageSize());
        }
        if (builder.sslContext() != null) {
            channelBuilder.negotiationType(NegotiationType.TLS);
            channelBuilder.sslContext(builder.sslContext());
        } else {
            channelBuilder.negotiationType(NegotiationType.PLAINTEXT);
        }

        if (builder.keepaliveTime() != null) {
            channelBuilder.keepAliveTime(builder.keepaliveTime().toMillis(), TimeUnit.MILLISECONDS);
        }
        if (builder.keepaliveTimeout() != null) {
            channelBuilder.keepAliveTimeout(builder.keepaliveTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        if (builder.keepaliveWithoutCalls() != null) {
            channelBuilder.keepAliveWithoutCalls(builder.keepaliveWithoutCalls());
        }
        if (builder.connectTimeout() != null) {
            channelBuilder.withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) builder.connectTimeout().toMillis());
        }

        if (builder.loadBalancerPolicy() != null) {
            channelBuilder.defaultLoadBalancingPolicy(builder.loadBalancerPolicy());
        } else {
            channelBuilder.defaultLoadBalancingPolicy("pick_first");
        }

        channelBuilder.intercept(new AuthTokenInterceptor());

        if (builder.headers() != null) {
            channelBuilder.intercept(new ClientInterceptor() {
                @Override
                public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions, Channel next) {
                    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                        @Override
                        public void start(Listener<RespT> responseListener, Metadata headers) {
                            builder.headers().forEach((BiConsumer<Metadata.Key, Object>) headers::put);
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
     * get token with ClientBuilder.
     *
     * @return                                              the auth token
     * @throws io.etcd.jetcd.common.exception.EtcdException a exception indicates failure reason.
     */
    @Nullable
    private String generateToken(Channel channel) {
        if (builder.user() != null && builder.password() != null) {
            try {
                return authenticate(channel, builder.user(), builder.password()).get().getToken();
            } catch (InterruptedException ite) {
                throw handleInterrupt(ite);
            } catch (ExecutionException exee) {
                throw toEtcdException(exee);
            }
        }
        return null;
    }

    /**
     * execute the task and retry it in case of failure.
     *
     * @param  task          a function that returns a new ListenableFuture.
     * @param  resultConvert a function that converts Type S to Type T.
     * @param  <S>           Source type
     * @param  <T>           Converted Type.
     * @return               a CompletableFuture with type T.
     */
    public <S, T> CompletableFuture<T> execute(Callable<ListenableFuture<S>> task, Function<S, T> resultConvert) {
        return execute(task, resultConvert, Util::isRetryable);
    }

    /**
     * execute the task and retry it in case of failure.
     *
     * @param  task          a function that returns a new SourceFuture.
     * @param  resultConvert a function that converts Type S to Type T.
     * @param  doRetry       a function that determines the retry condition base on SourceFuture error.
     * @param  <S>           Source type
     * @param  <T>           Converted Type.
     * @return               a CompletableFuture with type T.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public <S, T> CompletableFuture<T> execute(
        Callable<ListenableFuture<S>> task,
        Function<S, T> resultConvert,
        Predicate<Throwable> doRetry) {

        RetryPolicy<CompletableFuture<S>> retryPolicy = new RetryPolicy<CompletableFuture<S>>().handleIf(doRetry)
            .onRetriesExceeded(e -> LOGGER.warn("maximum number of auto retries reached"))
            .withBackoff(builder.retryDelay(), builder.retryMaxDelay(), builder.retryChronoUnit());

        if (builder.retryMaxDuration() != null) {
            retryPolicy = retryPolicy.withMaxDuration(builder.retryMaxDuration());
        }

        return Failsafe.with(retryPolicy).with(executorService)
            .getAsyncExecution(execution -> {
                CompletableFuture<S> wrappedFuture = new CompletableFuture<>();
                ListenableFuture<S> future = task.call();
                future.addListener(() -> {
                    try {
                        wrappedFuture.complete(future.get());
                        execution.complete(wrappedFuture);
                    } catch (Exception error) {
                        if (!execution.retryOn(error)) {
                            // permanent failure
                            wrappedFuture.completeExceptionally(error);
                        }
                    }
                }, executorService);
            }).thenCompose(f -> f.thenApply(resultConvert));
    }

    /**
     * AuthTokenInterceptor fills header with Auth token of any rpc calls and
     * refreshes token if the rpc results an invalid Auth token error.
     */
    private class AuthTokenInterceptor implements ClientInterceptor {

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
            return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    String token = getToken(next);
                    if (token != null) {
                        headers.put(TOKEN, token);
                    }
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
