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

package io.etcd.jetcd.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.support.Util;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.netty.NegotiationType;
import io.grpc.stub.AbstractStub;
import io.netty.channel.ChannelOption;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.grpc.VertxChannelBuilder;

import com.google.common.util.concurrent.ListenableFuture;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

final class ClientConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionManager.class);

    private final Object lock;
    private final ClientBuilder builder;
    private final ExecutorService executorService;
    private final AuthCredential credential;
    private volatile Vertx vertx;
    private volatile ManagedChannel managedChannel;

    ClientConnectionManager(ClientBuilder builder) {
        this(builder, null);
    }

    ClientConnectionManager(ClientBuilder builder, ManagedChannel managedChannel) {
        this.lock = new Object();
        this.builder = builder;
        this.managedChannel = managedChannel;
        this.credential = new AuthCredential(this);

        if (builder.executorService() == null) {
            // default to daemon
            this.executorService = Executors.newCachedThreadPool(Util.createThreadFactory("jetcd-", true));
        } else {
            this.executorService = builder.executorService();
        }
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

    ByteSequence getNamespace() {
        return builder.namespace();
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    ClientBuilder builder() {
        return builder;
    }

    AuthCredential authCredential() {
        return this.credential;
    }

    /**
     * create stub with saved channel.
     *
     * @param  supplier the stub supplier
     * @param  <T>      the type of stub
     * @return          the attached stub
     */
    <T extends AbstractStub<T>> T newStub(Function<ManagedChannel, T> supplier) {
        return newStub(supplier, getChannel());
    }

    private <T extends AbstractStub<T>> T newStub(Function<ManagedChannel, T> stubCustomizer, ManagedChannel channel) {
        T stub = stubCustomizer.apply(channel);
        if (builder.waitForReady()) {
            stub = stub.withWaitForReady();
        }
        if (builder.user() != null && builder.password() != null) {
            stub = stub.withCallCredentials(this.authCredential());
        }

        return stub;
    }

    void close() {
        synchronized (lock) {
            if (managedChannel != null) {
                managedChannel.shutdownNow();
            }
            if (vertx != null) {
                vertx.close();
            }
        }

        if (builder.executorService() == null) {
            executorService.shutdownNow();
        }
    }

    <T extends AbstractStub<T>, R> CompletableFuture<R> withNewChannel(
        String target,
        Function<ManagedChannel, T> stubCustomizer,
        Function<T, CompletableFuture<R>> stubConsumer) {

        final ManagedChannel channel = defaultChannelBuilder(target).build();
        final T stub = newStub(stubCustomizer, channel);

        try {
            return stubConsumer.apply(stub).whenComplete((r, t) -> channel.shutdown());
        } catch (Exception e) {
            channel.shutdown();
            throw toEtcdException(e);
        }
    }

    ManagedChannelBuilder<?> defaultChannelBuilder() {
        return defaultChannelBuilder(builder.target());
    }

    @SuppressWarnings("rawtypes")
    ManagedChannelBuilder<?> defaultChannelBuilder(String target) {
        if (target == null) {
            throw new IllegalArgumentException("At least one endpoint should be provided");
        }
        if (vertx == null) {
            vertx = Vertx.vertx(new VertxOptions().setUseDaemonThread(true));
        }
        final VertxChannelBuilder channelBuilder = VertxChannelBuilder.forTarget(vertx, target);

        if (builder.authority() != null) {
            channelBuilder.overrideAuthority(builder.authority());
        }
        if (builder.maxInboundMessageSize() != null) {
            channelBuilder.maxInboundMessageSize(builder.maxInboundMessageSize());
        }
        if (builder.sslContext() != null) {
            channelBuilder.nettyBuilder().negotiationType(NegotiationType.TLS);
            channelBuilder.nettyBuilder().sslContext(builder.sslContext());
        } else {
            channelBuilder.nettyBuilder().negotiationType(NegotiationType.PLAINTEXT);
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
            channelBuilder.nettyBuilder().withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                (int) builder.connectTimeout().toMillis());
        }

        if (builder.loadBalancerPolicy() != null) {
            channelBuilder.defaultLoadBalancingPolicy(builder.loadBalancerPolicy());
        } else {
            channelBuilder.defaultLoadBalancingPolicy("pick_first");
        }

        if (builder.headers() != null) {
            channelBuilder.intercept(new ClientInterceptor() {
                @Override
                public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions,
                    Channel next) {

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
                        if (Errors.isInvalidTokenError(error)) {
                            authCredential().refresh();
                        }
                        if (Errors.isAuthStoreExpired(error)) {
                            authCredential().refresh();
                        }
                        if (!execution.retryOn(error)) {
                            // permanent failure
                            wrappedFuture.completeExceptionally(error);
                        }
                    }
                }, executorService);
            }).thenCompose(f -> f.thenApply(resultConvert));
    }
}
