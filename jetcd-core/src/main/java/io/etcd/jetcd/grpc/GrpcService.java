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

package io.etcd.jetcd.grpc;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.common.suppliers.AsyncCloseable;
import io.etcd.jetcd.common.suppliers.AsyncCloseableSupplier;
import io.etcd.jetcd.common.suppliers.Suppliers;
import io.etcd.jetcd.resolver.ServiceResolver;
import io.etcd.jetcd.support.Util;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.endpoint.LoadBalancer;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientBuilder;
import io.vertx.grpc.client.GrpcClientOptions;
import io.vertx.grpc.client.GrpcClientRequestProvider;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

/**
 * Manages gRPC client services and lifecycle for etcd operations.
 * Provides access to base and authenticated gRPC clients with lazy initialization.
 */
public final class GrpcService {
    private static final int DEFAULT_MAX_INBOUND_MESSAGE_SIZE = 4 * 1024 * 1024;

    private final ClientBuilder builder;
    private final GrpcAuth auth;
    private final Vertx vertx;
    private final boolean closeVertx;
    private final AsyncCloseableSupplier<GrpcClientHolder> grpcClientSupplier;
    private final AsyncCloseableSupplier<GrpcClientHolder> authenticatedGrpcClientSupplier;
    private final AsyncCloseableSupplier<GrpcClientHolder> directGrpcClientSupplier;
    private boolean closing;
    private CompletableFuture<Void> closeFuture;

    /**
     * Creates a new GrpcService with the given builder.
     *
     * @param builder the client builder
     */
    public GrpcService(ClientBuilder builder) {
        this.builder = builder;
        this.closeVertx = builder.vertx() == null;
        this.vertx = builder.vertx() != null
            ? builder.vertx()
            : Vertx.vertx(new VertxOptions().setUseDaemonThread(true));
        this.auth = new GrpcAuth(this);

        // Use memoizing suppliers for thread-safe lazy initialization
        this.grpcClientSupplier = Suppliers.memoizingAsyncCloseable(
            () -> new GrpcClientHolder(createGrpcClient()));
        this.authenticatedGrpcClientSupplier = Suppliers.memoizingAsyncCloseable(
            () -> new GrpcClientHolder(auth.wrapWithAuth(getGrpcClient())));
        this.directGrpcClientSupplier = Suppliers.memoizingAsyncCloseable(
            () -> new GrpcClientHolder(grpcClientBuilder().build()));
    }

    /**
     * Returns the gRPC client for this service.
     *
     * @return the gRPC client
     */
    public synchronized GrpcClient getGrpcClient() {
        checkTransportOpen();
        return grpcClientSupplier.get().client();
    }

    /**
     * Get the authenticated GrpcClient that adds auth token headers to all requests.
     * Use this for authenticated operations that receive auth errors through gRPC status.
     *
     * @return the authenticated GrpcClient
     */
    public synchronized GrpcClient getAuthenticatedGrpcClient() {
        checkTransportOpen();
        return authenticatedGrpcClientSupplier.get().client();
    }

    private void checkTransportOpen() {
        if (closeFuture != null) {
            throw new ClosedClientException("Client is closing");
        }
    }

    /**
     * Binds authenticated requests to a member endpoint outside the cluster resolver.
     *
     * @param  target the member endpoint
     * @return        the bound request provider
     */
    public synchronized GrpcClientRequestProvider bindDirect(String target) {
        if (closing) {
            throw new ClosedClientException("Client is closing");
        }

        SocketAddress address = memberAddress(target);
        return auth.wrapWithAuth(directGrpcClientSupplier.get().client()).bind(address);
    }

    private SocketAddress memberAddress(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Member endpoint must include a host");
        }

        URI uri = URI.create(target.contains("://") ? target : "//" + target);
        String path = uri.getRawPath();
        if (uri.getHost() == null || uri.getPort() == 0 || uri.getPort() > 65535
            || uri.getRawAuthority().endsWith(":") || uri.getRawUserInfo() != null
            || uri.getRawQuery() != null || uri.getRawFragment() != null
            || (!path.isEmpty() && !path.equals("/"))) {
            throw new IllegalArgumentException("Invalid member endpoint");
        }

        String scheme = uri.getScheme();
        if (scheme != null) {
            boolean https = scheme.equalsIgnoreCase("https");
            if (!https && !scheme.equalsIgnoreCase("http")) {
                throw new IllegalArgumentException("Member endpoint scheme must be http or https");
            }
            HttpClientOptions options = builder.httpClientOptions();
            if (https != (options != null && options.isSsl())) {
                throw new IllegalArgumentException("Member endpoint scheme must match the client TLS setting");
            }
        }

        return Util.toSocketAddress(uri);
    }

    /**
     * Returns the service resolver for this service.
     *
     * @return the service resolver
     */
    public ServiceResolver<SocketAddress> getServiceResolver() {
        if (builder.serviceResolver() == null) {
            throw new IllegalArgumentException("EndpointResolver must be configured");
        }
        return builder.serviceResolver();
    }

    /**
     * Returns the namespace for this service.
     *
     * @return the namespace
     */
    public ByteSequence getNamespace() {
        return builder.namespace();
    }

    /**
     * Returns the client builder for this service.
     *
     * @return the client builder
     */
    public ClientBuilder builder() {
        return builder;
    }

    /**
     * Returns the auth service.
     *
     * @return the GrpcAuth instance
     */
    public GrpcAuth auth() {
        return this.auth;
    }

    /**
     * Returns the Vert.x instance used by this service.
     *
     * @return the Vert.x instance
     */
    public Vertx vertx() {
        return this.vertx;
    }

    /** Stops accepting direct calls while the client closes its services. */
    public synchronized void beginShutdown() {
        closing = true;
    }

    /**
     * Closes this service and releases all resources.
     *
     * @return a CompletableFuture that completes when the service is closed
     */
    public synchronized CompletableFuture<Void> close() {
        if (closeFuture != null) {
            return closeFuture;
        }

        closing = true;

        // Complete shutdown independently of the transport's event loop.
        Promise<Void> transportClosed = Promise.promise();
        closeFuture = transportClosed.future()
            .compose(v -> closeOwnedResources(),
                error -> closeOwnedResources().transform(ignored -> Future.failedFuture(error)))
            .toCompletionStage()
            .toCompletableFuture();
        Future.join(authenticatedGrpcClientSupplier.close(), grpcClientSupplier.close(), directGrpcClientSupplier.close())
            .<Void> mapEmpty()
            .onComplete(transportClosed);
        return closeFuture;
    }

    private Future<Void> closeOwnedResources() {
        auth.close();
        return closeVertx ? vertx.close() : Future.succeededFuture();
    }

    /**
     * Creates a temporary client with this service's transport options and executes the consumer.
     * The client is automatically closed after the consumer completes.
     *
     * @param  clientConsumer the consumer function
     * @param  <R>            the result type
     * @return                a CompletableFuture with the result
     */
    public <R> CompletableFuture<R> withNewClient(
        Function<GrpcClient, CompletableFuture<R>> clientConsumer) {

        final GrpcClient client = grpcClientBuilder().build();

        try {
            return clientConsumer.apply(client).whenComplete((r, t) -> client.close());
        } catch (Exception e) {
            client.close();
            throw toEtcdException(e);
        }
    }

    private GrpcClient createGrpcClient() {
        GrpcClientBuilder<GrpcClient> grpcBuilder = grpcClientBuilder();

        ServiceResolver<?> serviceResolver = getServiceResolver();
        grpcBuilder.withAddressResolver(serviceResolver.getResolver());

        LoadBalancer loadBalancer = builder.loadBalancer();
        if (loadBalancer == null) {
            loadBalancer = LoadBalancer.ROUND_ROBIN;
        }
        grpcBuilder.withLoadBalancer(loadBalancer);

        return grpcBuilder.build();
    }

    private GrpcClientBuilder<GrpcClient> grpcClientBuilder() {
        int maxMessageSize = Objects.requireNonNullElse(builder.maxInboundMessageSize(), DEFAULT_MAX_INBOUND_MESSAGE_SIZE);
        return GrpcClient.builder(vertx)
            .with(new GrpcClientOptions().setMaxMessageSize(maxMessageSize))
            .with(transportOptions(builder));
    }

    static HttpClientOptions transportOptions(ClientBuilder builder) {
        HttpClientOptions httpClientOptions = builder.httpClientOptions();
        Duration connectTimeout = builder.connectTimeout();
        if (connectTimeout == null) {
            return httpClientOptions;
        }

        int millis = connectTimeout.isZero() ? 0 : Math.max(1, Math.toIntExact(connectTimeout.toMillis()));
        var options = httpClientOptions == null ? new HttpClientOptions() : new HttpClientOptions(httpClientOptions);
        return options.setConnectTimeout(millis);
    }

    /**
     * Wrapper that adapts GrpcClient to AsyncCloseable for use with memoizing suppliers.
     */
    private static final class GrpcClientHolder implements AsyncCloseable {
        private final GrpcClient client;

        GrpcClientHolder(GrpcClient client) {
            this.client = client;
        }

        GrpcClient client() {
            return client;
        }

        @Override
        public Future<Void> close() {
            return client.close();
        }
    }
}
