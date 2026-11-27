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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.etcd.jetcd.common.Preconditions;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.common.vertx.Ssl;
import io.etcd.jetcd.impl.Client;
import io.etcd.jetcd.resolver.ServiceResolver;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.endpoint.LoadBalancer;

/**
 * ClientBuilder knows how to create a Client instance.
 */
public final class ClientBuilder implements Cloneable {

    private final ServiceResolver<SocketAddress> serviceResolver;
    private SecureByteSequence user;
    private SecureByteSequence password;
    private LoadBalancer loadBalancer;
    private Map<String, String> headers;
    private HttpClientOptions httpClientOptions;
    private Integer maxInboundMessageSize;
    private ByteSequence namespace = ByteSequence.EMPTY;
    private long retryDelay = 500;
    private long retryMaxDelay = 2500;
    private int retryMaxAttempts = 2;
    private ChronoUnit retryChronoUnit = ChronoUnit.MILLIS;
    private Duration keepaliveTime = Duration.ofSeconds(30L);
    private Duration leaseKeepAliveTimeout = Duration.ofSeconds(10L);
    private Boolean keepaliveWithoutCalls = true;
    private Duration retryMaxDuration;
    private Duration connectTimeout;
    private boolean waitForReady = true;
    private Vertx vertx;

    ClientBuilder(ServiceResolver<SocketAddress> serviceResolver) {
        this.serviceResolver = Preconditions.requireNonNull(serviceResolver, "serviceResolver cannot be null");
    }

    /**
     * Gets the endpoint resolver.
     *
     * @return the endpoint resolver.
     */
    public ServiceResolver<SocketAddress> serviceResolver() {
        return serviceResolver;
    }

    /**
     * Returns the auth user
     *
     * @return the user.
     */
    public ByteSequence user() {
        return user != null ? user.toByteSequence() : null;
    }

    /**
     * config etcd auth user with secure credential handling.
     *
     * @param  user                 etcd auth user as SecureByteSequence
     * @return                      this builder
     * @throws NullPointerException if user is <code>null</code>
     */
    public ClientBuilder user(SecureByteSequence user) {
        Objects.requireNonNull(user, "user can't be null");
        this.user = user;
        return this;
    }

    /**
     * Returns the auth password
     *
     * @return the password.
     */
    public ByteSequence password() {
        return password != null ? password.toByteSequence() : null;
    }

    /**
     * config etcd auth password with secure credential handling.
     *
     * @param  password             etcd auth password as SecureByteSequence
     * @return                      this builder
     * @throws NullPointerException if password is <code>null</code>
     */
    public ClientBuilder password(SecureByteSequence password) {
        Objects.requireNonNull(password, "password can't be null");
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
        Objects.requireNonNull(namespace, "namespace can't be null");
        this.namespace = namespace;
        return this;
    }

    /**
     * Returns the HTTP client options used for configuring the gRPC client.
     *
     * @return the HTTP client options, or null if not configured.
     */
    public HttpClientOptions httpClientOptions() {
        return httpClientOptions;
    }

    /**
     * Configure HTTP client options for the gRPC client.
     *
     * <p>
     * This allows full control over the HTTP/2 client configuration, including SSL/TLS settings,
     * connection pooling, timeouts, and other HTTP client behaviors.
     * </p>
     *
     * <p>
     * Example usage for SSL/TLS configuration:
     * </p>
     *
     * <pre>
     * import io.vertx.core.http.HttpClientOptions;
     * import io.vertx.core.net.PemTrustOptions;
     *
     * Client client = Client.builder("https://localhost:2379")
     *     .httpClientOptions(new HttpClientOptions()
     *         .setSsl(true)
     *         .setUseAlpn(true)
     *         .setTrustOptions(new PemTrustOptions().addCertPath("/path/to/ca.pem"))
     *         .setVerifyHost(false))
     *     .build();
     * </pre>
     *
     * @param  httpClientOptions the HTTP client options
     * @return                   this builder
     */
    public ClientBuilder httpClientOptions(HttpClientOptions httpClientOptions) {
        this.httpClientOptions = httpClientOptions;
        return this;
    }

    /**
     * Configure HTTP client options using a fluent consumer pattern.
     *
     * <p>
     * This method provides a convenient way to configure HTTP client options inline.
     * If HTTP client options were already set, this method will modify the existing
     * options rather than replacing them.
     * </p>
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * Client client = Client.builder("https://localhost:2379")
     *     .httpClientOptions(options -> options
     *         .setSsl(true)
     *         .setUseAlpn(true)
     *         .setTrustAll(true))
     *     .build();
     * </pre>
     *
     * <p>
     * For SSL/TLS configuration, see {@link Ssl} for helper methods:
     * </p>
     *
     * <pre>
     * import io.etcd.jetcd.common.vertx.SslUtil;
     *
     * Client client = Client.builder("https://localhost:2379")
     *     .httpClientOptions(SslUtil.withTrustManager("/path/to/ca.pem")
     *         .andThen(options -> options.setVerifyHost(false)))
     *     .build();
     * </pre>
     *
     * @param  consumer a consumer that configures the HttpClientOptions
     * @return          this builder
     * @see             Ssl
     */
    public ClientBuilder httpClientOptions(Consumer<HttpClientOptions> consumer) {
        // If options already exist, modify them. Otherwise create new ones.
        HttpClientOptions options = this.httpClientOptions != null ? this.httpClientOptions : new HttpClientOptions();
        consumer.accept(options);
        return httpClientOptions(options);
    }

    /**
     * Returns the configured maximum size for an inbound gRPC message.
     *
     * @return the maximum size in bytes, or null if the default of 4 MiB applies
     */
    public Integer maxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    /**
     * Sets the maximum size for an inbound gRPC message.
     *
     * @param  maxInboundMessageSize the positive maximum size in bytes, or null to use the default of 4 MiB
     * @return                       this builder
     */
    public ClientBuilder maxInboundMessageSize(Integer maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        return this;
    }

    /**
     * Sets the load balancer for distributing requests across etcd endpoints.
     *
     * <p>
     * Available strategies:
     * </p>
     * <ul>
     * <li>LoadBalancer.ROUND_ROBIN: Distributes requests evenly (default)</li>
     * <li>LoadBalancer.LEAST_REQUESTS: Routes to endpoint with fewest active requests</li>
     * <li>LoadBalancer.RANDOM: Random endpoint selection</li>
     * <li>LoadBalancer.POWER_OF_TWO_CHOICES: Picks best of two random endpoints</li>
     * </ul>
     *
     * @param  loadBalancer the load balancer instance
     * @return              this builder
     */
    public ClientBuilder loadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        return this;
    }

    /**
     * Returns the load balancer.
     *
     * @return the load balancer.
     */
    public LoadBalancer loadBalancer() {
        return loadBalancer;
    }

    /**
     * Returns the custom headers configured for all gRPC requests.
     *
     * @return an unmodifiable map of custom headers
     */
    public Map<String, String> headers() {
        return headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    }

    /**
     * Configure custom headers to be added to all gRPC requests.
     *
     * <p>
     * These headers will be added to every request made to etcd, including authentication
     * requests. Useful for adding correlation IDs, tracing headers, or other custom metadata.
     * </p>
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * Client client = Client.builder("http://localhost:2379")
     *     .header("X-Request-ID", "abc-123")
     *     .header("X-Trace-ID", "trace-456")
     *     .build();
     * </pre>
     *
     * @param  headers custom headers map
     * @return         this builder
     */
    public ClientBuilder headers(Map<String, String> headers) {
        this.headers = new HashMap<>(headers);
        return this;
    }

    /**
     * Add a single custom header to be included in all gRPC requests.
     *
     * @param  key   header name
     * @param  value header value
     * @return       this builder
     */
    public ClientBuilder header(String key, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(key, value);
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
     * Returns the legacy gRPC keepalive interval setting.
     *
     * @return     the legacy setting
     * @deprecated Vert.x gRPC does not expose periodic HTTP/2 keepalive pings as a client option.
     */
    @Deprecated
    public Duration keepaliveTime() {
        return keepaliveTime;
    }

    /**
     * Stores the legacy gRPC keepalive interval setting for compatibility.
     *
     * @param      keepaliveTime the legacy setting
     * @return                   this builder
     * @deprecated               Vert.x gRPC does not use this setting.
     */
    @Deprecated
    public ClientBuilder keepaliveTime(Duration keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
        return this;
    }

    /**
     * Returns the time allowed to receive a lease's first keepalive response.
     *
     * @return the configured timeout, or {@code null} for the fallback timeout
     */
    public Duration leaseKeepAliveTimeout() {
        return leaseKeepAliveTimeout;
    }

    /**
     * Sets the time allowed to receive a lease's first keepalive response.
     * Defaults to ten seconds. Pass {@code null} to use the five-second fallback.
     *
     * @param  timeout the initial lease keepalive timeout
     * @return         this builder
     */
    public ClientBuilder leaseKeepAliveTimeout(Duration timeout) {
        this.leaseKeepAliveTimeout = timeout;
        return this;
    }

    /**
     * Returns the time allowed to receive the first lease keepalive response.
     *
     * @return     the configured timeout
     * @deprecated Use {@link #leaseKeepAliveTimeout()}.
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public Duration keepaliveTimeout() {
        return leaseKeepAliveTimeout();
    }

    /**
     * Sets the time allowed to receive the first lease keepalive response.
     *
     * @param      keepaliveTimeout the initial lease keepalive timeout
     * @return                      this builder
     * @deprecated                  Use {@link #leaseKeepAliveTimeout(Duration)}.
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public ClientBuilder keepaliveTimeout(Duration keepaliveTimeout) {
        return leaseKeepAliveTimeout(keepaliveTimeout);
    }

    /**
     * Returns the legacy setting for sending gRPC keepalive pings without active calls.
     *
     * @return     the legacy setting
     * @deprecated Vert.x gRPC does not use this setting.
     */
    @Deprecated
    public Boolean keepaliveWithoutCalls() {
        return keepaliveWithoutCalls;
    }

    /**
     * Stores the legacy setting for sending gRPC keepalive pings without active calls.
     *
     * @param      keepaliveWithoutCalls the legacy setting
     * @return                           this builder
     * @deprecated                       Vert.x gRPC does not use this setting.
     */
    @Deprecated
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
     * Sets the TCP connection timeout for cluster and direct member connections. This setting takes precedence over
     * the timeout in {@link #httpClientOptions(HttpClientOptions)} regardless of call order.
     *
     * @param  connectTimeout           a non-negative duration of at most {@link Integer#MAX_VALUE} milliseconds.
     *                                  Positive durations below one millisecond use one millisecond. Zero disables the
     *                                  TCP connect timeout. Pass {@code null} to use the HTTP client options or defaults.
     * @return                          this builder
     * @throws IllegalArgumentException if the duration is negative or exceeds the supported range
     */
    public ClientBuilder connectTimeout(Duration connectTimeout) {
        if (connectTimeout != null
            && (connectTimeout.isNegative() || connectTimeout.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0)) {
            throw new IllegalArgumentException("connectTimeout must be between 0 and " + Integer.MAX_VALUE + " milliseconds");
        }
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Returns the legacy gRPC wait-for-ready setting.
     *
     * @return     the legacy setting
     * @deprecated Retry behavior is determined by the operation. Vert.x gRPC has no wait-for-ready option.
     */
    @Deprecated
    public boolean waitForReady() {
        return waitForReady;
    }

    /**
     * Stores the legacy gRPC wait-for-ready setting for compatibility.
     *
     * @param      waitForReady the legacy setting
     * @return                  this builder
     * @deprecated              Retry behavior is determined by the operation. Vert.x gRPC has no wait-for-ready
     *                          option.
     */
    @Deprecated
    public ClientBuilder waitForReady(boolean waitForReady) {
        this.waitForReady = waitForReady;
        return this;
    }

    /**
     * Gets the Vertx instance.
     *
     * @return the vertx instance.
     */
    public Vertx vertx() {
        return vertx;
    }

    /**
     * configure Vertx instance.
     *
     * @param  vertx                    Vertx instance to use.
     * @return                          this builder to train
     * @throws IllegalArgumentException if vertx is null
     */
    public ClientBuilder vertx(Vertx vertx) {
        Preconditions.checkArgument(vertx != null, "vertx can't be null");

        this.vertx = vertx;

        return this;
    }

    /**
     * build a new Client.
     *
     * @return               Client instance.
     * @throws EtcdException if client experiences build error.
     */
    public Client build() {
        return new Client(this);
    }

    /**
     * Returns a copy of this builder
     *
     * @return a copy of the builder.
     */
    public ClientBuilder copy() {
        try {
            ClientBuilder clone = (ClientBuilder) super.clone();
            // Deep copy the headers map to avoid shared mutable state
            if (this.headers != null) {
                clone.headers = new HashMap<>(this.headers);
            }
            // Deep copy HttpClientOptions to avoid shared mutable state
            if (this.httpClientOptions != null) {
                clone.httpClientOptions = new HttpClientOptions(this.httpClientOptions);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw EtcdExceptionFactory.toEtcdException(e);
        }
    }
}
