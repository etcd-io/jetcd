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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLException;

import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.impl.ClientImpl;
import io.etcd.jetcd.resolver.IPNameResolver;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Streams;

/**
 * ClientBuilder knows how to create a Client instance.
 */
public final class ClientBuilder implements Cloneable {

    private String target;
    private ByteSequence user;
    private ByteSequence password;
    private ExecutorService executorService;
    private String loadBalancerPolicy;
    private SslContext sslContext;
    private String authority;
    private Integer maxInboundMessageSize;
    private Map<Metadata.Key<?>, Object> headers;
    private Map<Metadata.Key<?>, Object> authHeaders;
    private List<ClientInterceptor> interceptors;
    private List<ClientInterceptor> authInterceptors;
    private ByteSequence namespace = ByteSequence.EMPTY;
    private long retryDelay = 500;
    private long retryMaxDelay = 2500;
    private int retryMaxAttempts = 2;
    private ChronoUnit retryChronoUnit = ChronoUnit.MILLIS;
    private Duration keepaliveTime = Duration.ofSeconds(30L);
    private Duration keepaliveTimeout = Duration.ofSeconds(10L);
    private Boolean keepaliveWithoutCalls = true;
    private Duration retryMaxDuration;
    private Duration connectTimeout;
    private boolean waitForReady = true;

    ClientBuilder() {
    }

    /**
     * Gets the etcd target.
     *
     * @return the etcd target.
     */
    public String target() {
        return target;
    }

    /**
     * configure etcd server endpoints.
     *
     * @param  target               etcd server target
     * @return                      this builder to train
     * @throws NullPointerException if target is null or one of endpoint is null
     */
    public ClientBuilder target(String target) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(target), "target can't be null or empty");

        this.target = target;

        return this;
    }

    /**
     * configure etcd server endpoints using the {@link IPNameResolver}.
     *
     * @param  endpoints                etcd server endpoints, at least one
     * @return                          this builder to train
     * @throws NullPointerException     if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if some endpoint is invalid
     */
    public ClientBuilder endpoints(String... endpoints) {
        return endpoints(
            Stream.of(endpoints).map(URI::create).toArray(URI[]::new));
    }

    /**
     * configure etcd server endpoints using the {@link IPNameResolver}.
     *
     * @param  endpoints                etcd server endpoints, at least one
     * @return                          this builder to train
     * @throws NullPointerException     if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if some endpoint is invalid
     */
    public ClientBuilder endpoints(URI... endpoints) {
        return endpoints(Arrays.asList(endpoints));
    }

    /**
     * configure etcd server endpoints using the {@link IPNameResolver}.
     *
     * @param  endpoints                etcd server endpoints, at least one
     * @return                          this builder to train
     * @throws NullPointerException     if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if some endpoint is invalid
     */
    public ClientBuilder endpoints(Iterable<URI> endpoints) {
        Preconditions.checkNotNull(endpoints, "endpoints can't be null");

        endpoints.forEach(e -> {
            if (e.getHost() == null) {
                throw new IllegalArgumentException("Unable to compute target from endpoint: '" + e + "'");
            }
        });

        final String target = Streams.stream(endpoints)
            .map(e -> e.getHost() + (e.getPort() != -1 ? ":" + e.getPort() : ""))
            .distinct()
            .collect(Collectors.joining(","));

        if (Strings.isNullOrEmpty(target)) {
            throw new IllegalArgumentException("Unable to compute target from endpoints: '" + endpoints + "'");
        }

        return target(
            String.format(
                "%s://%s/%s",
                IPNameResolver.SCHEME,
                authority != null ? authority : "",
                target));
    }

    /**
     * Returns the auth user
     *
     * @return the user.
     */
    public ByteSequence user() {
        return user;
    }

    /**
     * config etcd auth user.
     *
     * @param  user                 etcd auth user
     * @return                      this builder
     * @throws NullPointerException if user is <code>null</code>
     */
    public ClientBuilder user(ByteSequence user) {
        Preconditions.checkNotNull(user, "user can't be null");
        this.user = user;
        return this;
    }

    /**
     * Returns the auth password
     *
     * @return the password.
     */
    public ByteSequence password() {
        return password;
    }

    /**
     * config etcd auth password.
     *
     * @param  password             etcd auth password
     * @return                      this builder
     * @throws NullPointerException if password is <code>null</code>
     */
    public ClientBuilder password(ByteSequence password) {
        Preconditions.checkNotNull(password, "password can't be null");
        this.password = password;
        return this;
    }

    /**
     * Returns the namespace of each key used
     *
     * @return the namespace.
     */
    public ByteSequence namespace() {
        return namespace;
    }

    /**
     * config the namespace of keys used in {@code KV}, {@code Txn}, {@code Lock} and {@code Watch}.
     * "/" will be treated as no namespace.
     *
     * @param  namespace            the namespace of each key used
     * @return                      this builder
     * @throws NullPointerException if namespace is <code>null</code>
     */
    public ClientBuilder namespace(ByteSequence namespace) {
        Preconditions.checkNotNull(namespace, "namespace can't be null");
        this.namespace = namespace;
        return this;
    }

    /**
     * Returns the executor service
     *
     * @return the executor service.
     */
    public ExecutorService executorService() {
        return executorService;
    }

    /**
     * config executor service.
     *
     * @param  executorService      executor service
     * @return                      this builder
     * @throws NullPointerException if executorService is <code>null</code>
     */
    public ClientBuilder executorService(ExecutorService executorService) {
        Preconditions.checkNotNull(executorService, "executorService can't be null");
        this.executorService = executorService;
        return this;
    }

    /**
     * config load balancer policy.
     *
     * @param  loadBalancerPolicy   etcd load balancer policy
     * @return                      this builder
     * @throws NullPointerException if loadBalancerPolicy is <code>null</code>
     */
    public ClientBuilder loadBalancerPolicy(String loadBalancerPolicy) {
        Preconditions.checkNotNull(loadBalancerPolicy, "loadBalancerPolicy can't be null");
        this.loadBalancerPolicy = loadBalancerPolicy;
        return this;
    }

    /**
     * get the load balancer policy for etcd client.
     *
     * @return loadBalancerFactory
     */
    public String loadBalancerPolicy() {
        return loadBalancerPolicy;
    }

    /**
     * Returns the ssl context
     *
     * @return the ssl context.
     */
    public SslContext sslContext() {
        return sslContext;
    }

    /**
     * SSL/TLS context to use instead of the system default. It must have been configured with {@link
     * GrpcSslContexts}, but options could have been overridden.
     *
     * @param  sslContext the ssl context
     * @return            this builder
     */
    public ClientBuilder sslContext(SslContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    /**
     * Configure SSL/TLS context create through {@link GrpcSslContexts#forClient} to use.
     *
     * @param  consumer     the SslContextBuilder consumer
     * @return              this builder
     * @throws SSLException if the SslContextBuilder fails
     */
    public ClientBuilder sslContext(Consumer<SslContextBuilder> consumer) throws SSLException {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        consumer.accept(builder);

        return sslContext(builder.build());
    }

    /**
     * Returns The authority used to authenticate connections to servers.
     *
     * @return the authority.
     */
    public String authority() {
        return authority;
    }

    /**
     * Sets the authority used to authenticate connections to servers.
     *
     * @param  authority the authority used to authenticate connections to servers.
     * @return           this builder
     */
    public ClientBuilder authority(String authority) {
        this.authority = authority;
        return this;
    }

    /**
     * Returns the maximum message size allowed for a single gRPC frame.
     *
     * @return max inbound message size.
     */
    public Integer maxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    /**
     * Sets the maximum message size allowed for a single gRPC frame.
     *
     * @param  maxInboundMessageSize the maximum message size allowed for a single gRPC frame.
     * @return                       this builder
     */
    public ClientBuilder maxInboundMessageSize(Integer maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        return this;
    }

    /**
     * Returns the headers to be added to http request headers
     *
     * @return headers.
     */
    public Map<Metadata.Key<?>, Object> headers() {
        return headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    }

    /**
     * Sets headers to be added to http request headers.
     *
     * @param  headers headers to be added to http request headers.
     * @return         this builder
     */
    public ClientBuilder headers(Map<Metadata.Key<?>, Object> headers) {
        this.headers = new HashMap<>(headers);

        return this;
    }

    /**
     * Set headers.
     *
     * @param  key   Sets an header key to be added to http request headers.
     * @param  value Sets an header value to be added to http request headers.
     * @return       this builder
     */
    public ClientBuilder header(String key, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }

        this.headers.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);

        return this;
    }

    /**
     * Returns the headers to be added to auth request headers
     *
     * @return auth headers.
     */
    public Map<Metadata.Key<?>, Object> authHeaders() {
        return authHeaders == null ? Collections.emptyMap() : Collections.unmodifiableMap(authHeaders);
    }

    /**
     * Set the auth headers.
     *
     * @param  authHeaders Sets headers to be added to auth request headers.
     * @return             this builder
     */
    public ClientBuilder authHeaders(Map<Metadata.Key<?>, Object> authHeaders) {
        this.authHeaders = new HashMap<>(authHeaders);

        return this;
    }

    /**
     * Add an auth header.
     *
     * @param  key   Sets an header key to be added to auth request headers.
     * @param  value Sets an header value to be added to auth request headers.
     * @return       this builder
     */
    public ClientBuilder authHeader(String key, String value) {
        if (this.authHeaders == null) {
            this.authHeaders = new HashMap<>();
        }

        this.authHeaders.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);

        return this;
    }

    /**
     * Returns the interceptors
     *
     * @return the interceptors.
     */
    public List<ClientInterceptor> interceptors() {
        return interceptors;
    }

    /**
     * Set the interceptors.
     *
     * @param  interceptors the interceptors.
     * @return              this builder
     */
    public ClientBuilder interceptors(List<ClientInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);

        return this;
    }

    /**
     * Add an interceptor.
     *
     * @param  interceptor  an interceptors to add
     * @param  interceptors additional interceptors
     * @return              this builder
     */
    public ClientBuilder interceptor(ClientInterceptor interceptor, ClientInterceptor... interceptors) {
        if (this.interceptors == null) {
            this.interceptors = new ArrayList<>();
        }

        this.interceptors.add(interceptor);
        this.interceptors.addAll(Arrays.asList(interceptors));

        return this;
    }

    /**
     * Returns the auth interceptors
     *
     * @return the interceptors.
     */
    public List<ClientInterceptor> authInterceptors() {
        return authInterceptors;
    }

    /**
     * Set the auth interceptors.
     *
     * @param  interceptors Set the interceptors to add to the auth chain
     * @return              this builder
     */
    public ClientBuilder authInterceptors(List<ClientInterceptor> interceptors) {
        this.authInterceptors = new ArrayList<>(interceptors);

        return this;
    }

    /**
     * Add an auth interceptor.
     *
     * @param  interceptor  an interceptors to add to the auth chain
     * @param  interceptors additional interceptors to add to the auth chain
     * @return              this builder
     */
    public ClientBuilder authInterceptors(ClientInterceptor interceptor, ClientInterceptor... interceptors) {
        if (this.authInterceptors == null) {
            this.authInterceptors = new ArrayList<>();
        }

        this.authInterceptors.add(interceptor);
        this.authInterceptors.addAll(Arrays.asList(interceptors));

        return this;
    }

    /**
     * Returns The delay between retries.
     *
     * @return the retry delay.
     */
    public long retryDelay() {
        return retryDelay;
    }

    /**
     * The delay between retries.
     *
     * @param  retryDelay The delay between retries.
     * @return            this builder
     */
    public ClientBuilder retryDelay(long retryDelay) {
        this.retryDelay = retryDelay;
        return this;
    }

    /**
     * Returns the max backing off delay between retries
     *
     * @return max retry delay.
     */
    public long retryMaxDelay() {
        return retryMaxDelay;
    }

    /**
     * Set the max backing off delay between retries.
     *
     * @param  retryMaxDelay The max backing off delay between retries.
     * @return               this builder
     */
    public ClientBuilder retryMaxDelay(long retryMaxDelay) {
        this.retryMaxDelay = retryMaxDelay;
        return this;
    }

    /**
     * Returns the max number of retry attempts
     *
     * @return max retry attempts.
     */
    public int retryMaxAttempts() {
        return retryMaxAttempts;
    }

    /**
     * Set the max number of retry attempts
     *
     * @param  retryMaxAttempts The max retry attempts.
     * @return                  this builder
     */
    public ClientBuilder retryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
        return this;
    }

    /**
     * Returns the keep alive time.
     *
     * @return keep alive time.
     */
    public Duration keepaliveTime() {
        return keepaliveTime;
    }

    /**
     * The interval for gRPC keepalives.
     * The current minimum allowed by gRPC is 10s
     *
     * @param  keepaliveTime time between keepalives
     * @return               this builder
     */
    public ClientBuilder keepaliveTime(Duration keepaliveTime) {
        // gRPC uses a minimum keepalive time of 10s, if smaller values are given.
        // No check here though, as this gRPC value might change
        this.keepaliveTime = keepaliveTime;
        return this;
    }

    /**
     * Returns the keep alive time out.
     *
     * @return keep alive time out.
     */
    public Duration keepaliveTimeout() {
        return keepaliveTimeout;
    }

    /**
     * The timeout for gRPC keepalives
     *
     * @param  keepaliveTimeout the gRPC keep alive timeout.
     * @return                  this builder
     */
    public ClientBuilder keepaliveTimeout(Duration keepaliveTimeout) {
        this.keepaliveTimeout = keepaliveTimeout;
        return this;
    }

    public Boolean keepaliveWithoutCalls() {
        return keepaliveWithoutCalls;
    }

    /**
     * Keepalive option for gRPC
     *
     * @param  keepaliveWithoutCalls the gRPC keep alive without calls.
     * @return                       this builder
     */
    public ClientBuilder keepaliveWithoutCalls(Boolean keepaliveWithoutCalls) {
        this.keepaliveWithoutCalls = keepaliveWithoutCalls;
        return this;
    }

    /**
     * Returns he retries period unit.
     *
     * @return the chrono unit.
     */
    public ChronoUnit retryChronoUnit() {
        return retryChronoUnit;
    }

    /**
     * Sets the retries period unit.
     *
     * @param  retryChronoUnit the retries period unit.
     * @return                 this builder
     */
    public ClientBuilder retryChronoUnit(ChronoUnit retryChronoUnit) {
        this.retryChronoUnit = retryChronoUnit;
        return this;
    }

    /**
     * Returns the retries max duration.
     *
     * @return retry max duration.
     */
    public Duration retryMaxDuration() {
        return retryMaxDuration;
    }

    /**
     * Returns the connect timeout.
     *
     * @return connect timeout.
     */
    public Duration connectTimeout() {
        return connectTimeout;
    }

    /**
     * Set the retries max duration.
     *
     * @param  retryMaxDuration the retries max duration.
     * @return                  this builder
     */
    public ClientBuilder retryMaxDuration(Duration retryMaxDuration) {
        this.retryMaxDuration = retryMaxDuration;
        return this;
    }

    /**
     * Set the connection timeout.
     *
     * @param  connectTimeout Sets the connection timeout.
     *                        Clients connecting to fault tolerant etcd clusters (eg, clusters with more than 2 etcd server
     *                        peers/endpoints)
     *                        should consider a value that will allow switching timely from a crashed/partitioned peer to
     *                        a consensus peer.
     * @return                this builder
     */
    public ClientBuilder connectTimeout(Duration connectTimeout) {
        if (connectTimeout != null) {
            long millis = connectTimeout.toMillis();
            if ((int) millis != millis) {
                throw new IllegalArgumentException("connectTimeout outside of its bounds, max value: " +
                    Integer.MAX_VALUE);
            }
        }
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Enable gRPC's wait for ready semantics.
     *
     * @return if this client uses gRPC's wait for ready semantics.
     * @see    <a href="https://github.com/grpc/grpc/blob/master/doc/wait-for-ready.md">gRPC Wait for Ready Semantics</a>
     */
    public boolean waitForReady() {
        return waitForReady;
    }

    /**
     * Configure the gRPC's wait for ready semantics.
     *
     * @param  waitForReady if this client should use gRPC's wait for ready semantics. Enabled by default.
     * @return              this builder.
     * @see                 <a href="https://github.com/grpc/grpc/blob/master/doc/wait-for-ready.md">gRPC Wait for Ready
     *                      Semantics</a>
     */
    public ClientBuilder waitForReady(boolean waitForReady) {
        this.waitForReady = waitForReady;
        return this;
    }

    /**
     * build a new Client.
     *
     * @return               Client instance.
     * @throws EtcdException if client experiences build error.
     */
    public Client build() {
        Preconditions.checkState(target != null, "please configure etcd server endpoints before build.");

        return new ClientImpl(this);
    }

    /**
     * Returns a copy of this builder
     *
     * @return a copy of the builder.
     */
    public ClientBuilder copy() {
        try {
            return (ClientBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw EtcdExceptionFactory.toEtcdException(e);
        }
    }
}
