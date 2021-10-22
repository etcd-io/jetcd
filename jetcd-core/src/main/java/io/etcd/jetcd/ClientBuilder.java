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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * ClientBuilder knows how to create an Client instance.
 */
public final class ClientBuilder implements Cloneable {

    private final Set<URI> endpoints = new HashSet<>();
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
    private ChronoUnit retryChronoUnit = ChronoUnit.MILLIS;
    private Duration keepaliveTime = Duration.ofSeconds(30L);
    private Duration keepaliveTimeout = Duration.ofSeconds(10L);
    private Boolean keepaliveWithoutCalls = true;
    private Duration retryMaxDuration;
    private Duration connectTimeout;
    private boolean discovery;

    ClientBuilder() {
    }

    /**
     * gets the endpoints for the builder.
     *
     * @return the list of endpoints configured for the builder
     */
    public Collection<URI> endpoints() {
        return Collections.unmodifiableCollection(this.endpoints);
    }

    /**
     * configure etcd server endpoints.
     *
     * @param  endpoints                etcd server endpoints, at least one
     * @return                          this builder to train
     * @throws NullPointerException     if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if some endpoint is invalid
     */
    public ClientBuilder endpoints(Collection<URI> endpoints) {
        checkNotNull(endpoints, "endpoints can't be null");

        for (URI endpoint : endpoints) {
            checkNotNull(endpoint, "endpoint can't be null");
            checkArgument(endpoint.toString().trim().length() > 0, "invalid endpoint: endpoint=" + endpoint);
            this.endpoints.add(endpoint);
        }

        return this;
    }

    /**
     * configure etcd server endpoints.
     *
     * @param  endpoints                etcd server endpoints, at least one
     * @return                          this builder to train
     * @throws NullPointerException     if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if some endpoint is invalid
     */
    public ClientBuilder endpoints(URI... endpoints) {
        checkNotNull(endpoints, "endpoints can't be null");

        return endpoints(Arrays.asList(endpoints));
    }

    /**
     * configure etcd server endpoints.
     *
     * @param  endpoints                etcd server endpoints, at least one
     * @return                          this builder to train
     * @throws NullPointerException     if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if some endpoint is invalid
     */
    public ClientBuilder endpoints(String... endpoints) {
        return endpoints(Util.toURIs(Arrays.asList(endpoints)));
    }

    /**
     * @return the auth user
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
        checkNotNull(user, "user can't be null");
        this.user = user;
        return this;
    }

    /**
     * @return the auth password
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
        checkNotNull(password, "password can't be null");
        this.password = password;
        return this;
    }

    /**
     * @return the namespace of each key used
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
        checkNotNull(namespace, "namespace can't be null");
        this.namespace = namespace;
        return this;
    }

    /**
     * @return the executor service
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
        checkNotNull(executorService, "executorService can't be null");
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
        checkNotNull(loadBalancerPolicy, "loadBalancerPolicy can't be null");
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
     * @return the ssl context
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
     * @return The authority used to authenticate connections to servers.
     */
    public String authority() {
        return authority;
    }

    /**
     * @param  authority the authority used to authenticate connections to servers.
     * @return           this builder
     */
    public ClientBuilder authority(String authority) {
        this.authority = authority;
        return this;
    }

    /**
     * @return the maximum message size allowed for a single gRPC frame.
     */
    public Integer maxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    /**
     * @param  maxInboundMessageSize Sets the maximum message size allowed for a single gRPC frame.
     * @return                       this builder
     */
    public ClientBuilder maxInboundMessageSize(Integer maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        return this;
    }

    /**
     * @return the headers to be added to http request headers
     */
    public Map<Metadata.Key<?>, Object> headers() {
        return headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    }

    /**
     * @param  headers Sets headers to be added to http request headers.
     * @return         this builder
     */
    public ClientBuilder headers(Map<Metadata.Key<?>, Object> headers) {
        this.headers = new HashMap<>(headers);

        return this;
    }

    /**
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
     * @return the headers to be added to auth request headers
     */
    public Map<Metadata.Key<?>, Object> authHeaders() {
        return authHeaders == null ? Collections.emptyMap() : Collections.unmodifiableMap(authHeaders);
    }

    /**
     * @param  authHeaders Sets headers to be added to auth request headers.
     * @return             this builder
     */
    public ClientBuilder authHeaders(Map<Metadata.Key<?>, Object> authHeaders) {
        this.authHeaders = new HashMap<>(authHeaders);

        return this;
    }

    /**
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
     * @return the interceptors
     */
    public List<ClientInterceptor> interceptors() {
        return interceptors;
    }

    /**
     * @param  interceptors Set the interceptors.
     * @return              this builder
     */
    public ClientBuilder interceptors(List<ClientInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);

        return this;
    }

    /**
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
     * @return the auth interceptors
     */
    public List<ClientInterceptor> authInterceptors() {
        return authInterceptors;
    }

    /**
     * @param  interceptors Set the interceptors to add to the auth chain
     * @return              this builder
     */
    public ClientBuilder authInterceptors(List<ClientInterceptor> interceptors) {
        this.authInterceptors = new ArrayList<>(interceptors);

        return this;
    }

    /**
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
     * @return The delay between retries.
     */
    public long retryDelay() {
        return retryDelay;
    }

    /**
     * @param  retryDelay The delay between retries.
     * @return            this builder
     */
    public ClientBuilder retryDelay(long retryDelay) {
        this.retryDelay = retryDelay;
        return this;
    }

    /**
     * @return the max backing off delay between retries
     */
    public long retryMaxDelay() {
        return retryMaxDelay;
    }

    /**
     * @param  retryMaxDelay The max backing off delay between retries.
     * @return               this builder
     */
    public ClientBuilder retryMaxDelay(long retryMaxDelay) {
        this.retryMaxDelay = retryMaxDelay;
        return this;
    }

    public Duration keepaliveTime() {
        return keepaliveTime;
    }

    /**
     * The interval for gRPC keepalives.
     * The current minimum allowed by gRPC is 10s
     *
     * @param keepaliveTime time between keepalives
     */
    public ClientBuilder keepaliveTime(Duration keepaliveTime) {
        // gRPC uses a minimum keepalive time of 10s, if smaller values are given.
        // No check here though, as this gRPC value might change
        this.keepaliveTime = keepaliveTime;
        return this;
    }

    public Duration keepaliveTimeout() {
        return keepaliveTimeout;
    }

    /**
     * The timeout for gRPC keepalives
     *
     * @param keepaliveTimeout the gRPC keep alive timeout.
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
     * @param keepaliveWithoutCalls the gRPC keep alive without calls.
     */
    public ClientBuilder keepaliveWithoutCalls(Boolean keepaliveWithoutCalls) {
        this.keepaliveWithoutCalls = keepaliveWithoutCalls;
        return this;
    }

    /**
     * @return he retries period unit.
     */
    public ChronoUnit retryChronoUnit() {
        return retryChronoUnit;
    }

    /**
     * @param  retryChronoUnit the retries period unit.
     * @return                 this builder
     */
    public ClientBuilder retryChronoUnit(ChronoUnit retryChronoUnit) {
        this.retryChronoUnit = retryChronoUnit;
        return this;
    }

    /**
     * @return the retries max duration.
     */
    public Duration retryMaxDuration() {
        return retryMaxDuration;
    }

    /**
     * @return the connect timeout.
     */
    public Duration connectTimeout() {
        return connectTimeout;
    }

    /**
     * @param  retryMaxDuration the retries max duration.
     * @return                  this builder
     */
    public ClientBuilder retryMaxDuration(Duration retryMaxDuration) {
        this.retryMaxDuration = retryMaxDuration;
        return this;
    }

    /**
     * @param  connectTimeout Sets the connection timeout.
     *                        Clients connecting to fault tolerant etcd clusters (eg, clusters with >= 3 etcd server
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
     * @return if the endpoint represent a discovery address using dns+srv.
     */
    public boolean discovery() {
        return discovery;
    }

    /**
     * @param  discovery if the endpoint represent a discovery address using dns+srv.
     * @return           this builder
     */
    public ClientBuilder discovery(boolean discovery) {
        this.discovery = discovery;
        return this;
    }

    /**
     * build a new Client.
     *
     * @return               Client instance.
     * @throws EtcdException if client experiences build error.
     */
    public Client build() {
        checkState(!endpoints.isEmpty(), "please configure etcd server endpoints before build.");

        return new ClientImpl(this);
    }

    /**
     * @return a copy of this builder
     */
    public ClientBuilder copy() {
        try {
            return (ClientBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw EtcdExceptionFactory.toEtcdException(e);
        }
    }
}
