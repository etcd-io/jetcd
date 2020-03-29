/*
 * Copyright 2016-2020 The jetcd authors
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
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.etcd.jetcd.api.AuthGrpc;
import io.etcd.jetcd.api.AuthenticateRequest;
import io.etcd.jetcd.api.AuthenticateResponse;
import io.etcd.jetcd.common.exception.ErrorCode;
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
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import static com.google.common.base.Preconditions.checkArgument;
import static io.etcd.jetcd.Util.isInvalidTokenError;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.handleInterrupt;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static io.etcd.jetcd.resolver.SmartNameResolverFactory.forEndpoints;

final class ClientConnectionManager {

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
        return supplier.apply(getChannel());
    }

    void close() {
        synchronized (lock) {
            if (managedChannel != null) {
                managedChannel.shutdownNow();
            }
        }

        executorService.shutdownNow();
    }

    <T extends AbstractStub<T>, R> CompletableFuture<R> withNewChannel(URI endpoint, Function<ManagedChannel, T> stubCustomizer,
        Function<T, CompletableFuture<R>> stubConsumer) {

        final ManagedChannel channel = defaultChannelBuilder().nameResolverFactory(
            forEndpoints(
                Util.supplyIfNull(builder.authority(), () -> ""),
                Collections.singleton(endpoint),
                Util.supplyIfNull(builder.uriResolverLoader(), URIResolverLoader::defaultLoader)))
            .build();

        try {
            T stub = stubCustomizer.apply(channel);

            return stubConsumer.apply(stub).whenComplete((r, t) -> channel.shutdown());
        } catch (Exception e) {
            channel.shutdown();
            throw EtcdExceptionFactory.toEtcdException(e);
        }
    }

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
                Util.supplyIfNull(builder.authority(), () -> ""),
                builder.endpoints(),
                Util.supplyIfNull(builder.uriResolverLoader(),
                    URIResolverLoader::defaultLoader)));

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
    public <S, T> CompletableFuture<T> execute(Callable<ListenableFuture<S>> task, Function<S, T> resultConvert,
        Predicate<Throwable> doRetry) {

        RetryPolicy<S> retryPolicy = new RetryPolicy<S>().handleIf(doRetry)
            .onRetriesExceeded(e -> newEtcdException(ErrorCode.ABORTED, "maximum number of auto retries reached"))
            .withBackoff(builder.retryDelay(), builder.retryMaxDelay(), builder.retryChronoUnit());

        if (builder.retryMaxDuration() != null) {
            retryPolicy = retryPolicy.withMaxDuration(Duration.parse(builder.retryMaxDuration()));
        }

        return Failsafe.with(retryPolicy).with(executorService).getAsync(() -> task.call().get()).thenApply(resultConvert);
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
