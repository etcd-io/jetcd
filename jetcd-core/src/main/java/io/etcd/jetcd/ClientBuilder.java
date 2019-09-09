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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.resolver.URIResolverLoader;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import java.net.URI;
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
  private boolean lazyInitialization = false;
  private String authority;
  private URIResolverLoader uriResolverLoader;
  private Integer maxInboundMessageSize;
  private Map<Metadata.Key, Object> headers;
  private List<ClientInterceptor> interceptors;
  private ByteSequence namespace = ByteSequence.EMPTY;
  private long retryDelay = 500;
  private long retryMaxDelay = 2500;
  private ChronoUnit retryChronoUnit = ChronoUnit.MILLIS;
  private String retryMaxDuration;

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
   * @param endpoints etcd server endpoints, at least one
   * @return this builder to train
   * @throws NullPointerException if endpoints is null or one of endpoint is null
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
   * @param endpoints etcd server endpoints, at least one
   * @return this builder to train
   * @throws NullPointerException if endpoints is null or one of endpoint is null
   * @throws IllegalArgumentException if some endpoint is invalid
   */
  public ClientBuilder endpoints(URI... endpoints) {
    checkNotNull(endpoints, "endpoints can't be null");

    return endpoints(Arrays.asList(endpoints));
  }

  public ClientBuilder endpoints(String... endpoints) {
    return endpoints(Util.toURIs(Arrays.asList(endpoints)));
  }

  public ByteSequence user() {
    return user;
  }

  /**
   * config etcd auth user.
   *
   * @param user etcd auth user
   * @return this builder
   * @throws NullPointerException if user is <code>null</code>
   */
  public ClientBuilder user(ByteSequence user) {
    checkNotNull(user, "user can't be null");
    this.user = user;
    return this;
  }

  public ByteSequence password() {
    return password;
  }

  /**
   * config etcd auth password.
   *
   * @param password etcd auth password
   * @return this builder
   * @throws NullPointerException if password is <code>null</code>
   */
  public ClientBuilder password(ByteSequence password) {
    checkNotNull(password, "password can't be null");
    this.password = password;
    return this;
  }

  public ByteSequence namespace() {
    return namespace;
  }

  /**
   * config the namespace of keys used in {@code KV}, {@code Txn}, {@code Lock} and {@code Watch}.
   * "/" will be treated as no namespace.
   *
   * @param namespace the namespace of each key used
   * @return this builder
   * @throws NullPointerException if namespace is <code>null</code>
   */
  public ClientBuilder namespace(ByteSequence namespace) {
    checkNotNull(namespace, "namespace can't be null");
    this.namespace = namespace;
    return this;
  }

  public ExecutorService executorService() {
    return executorService;
  }

  /**
   * config executor service.
   *
   * @param executorService executor service
   * @return this builder
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
   * @param loadBalancerPolicy etcd load balancer policy
   * @return this builder
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

  public boolean lazyInitialization() {
    return lazyInitialization;
  }

  /**
   * Define if the client has to initialize connectivity and authentication on client constructor
   * or delay it to the first call to a client. Default is false.
   *
   * @param lazyInitialization true if the client has to lazily perform
   *        connectivity/authentication.
   * @return this builder
   */
  public ClientBuilder lazyInitialization(boolean lazyInitialization) {
    this.lazyInitialization = lazyInitialization;
    return this;
  }

  public SslContext sslContext() {
    return sslContext;
  }

  /**
   * SSL/TLS context to use instead of the system default. It must have been configured with {@link
   * GrpcSslContexts}, but options could have been overridden.
   *
   * @param sslContext the ssl context
   * @return this builder
   */
  public ClientBuilder sslContext(SslContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  public String authority() {
    return authority;
  }

  /**
   * The authority used to authenticate connections to servers.
   */
  public ClientBuilder authority(String authority) {
    this.authority = authority;
    return this;
  }

  public URIResolverLoader uriResolverLoader() {
    return uriResolverLoader;
  }

  public ClientBuilder uriResolverLoader(URIResolverLoader loader) {
    this.uriResolverLoader = loader;
    return this;
  }

  public Integer maxInboundMessageSize() {
    return maxInboundMessageSize;
  }

  /**
   * Sets the maximum message size allowed for a single gRPC frame.
   */
  public ClientBuilder maxInboundMessageSize(Integer maxInboundMessageSize) {
    this.maxInboundMessageSize = maxInboundMessageSize;
    return this;
  }

  public Map<Metadata.Key, Object> headers() {
    return headers;
  }

  /**
   * Sets headers to be added to http request headers.
   */
  public ClientBuilder headers(Map<Metadata.Key, Object> headers) {
    this.headers = new HashMap<>(headers);

    return this;
  }

  /**
   * Sets an header to be added to http request headers.
   */
  public ClientBuilder header(String key, String value) {
    if (this.headers == null) {
      this.headers = new HashMap<>();
    }

    this.headers.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);

    return this;
  }

  public List<ClientInterceptor> interceptors() {
    return interceptors;
  }

  /**
   * Set the interceptors.
   */
  public ClientBuilder interceptors(List<ClientInterceptor> interceptors) {
    this.interceptors = new ArrayList<>(interceptors);

    return this;
  }

  /**
   * Add interceptors.
   */
  public ClientBuilder interceptor(ClientInterceptor interceptor, ClientInterceptor... interceptors) {
    if (this.interceptors == null) {
      this.interceptors = new ArrayList<>();
    }

    this.interceptors.add(interceptor);

    for (ClientInterceptor i: interceptors) {
      this.interceptors.add(i);
    }

    return this;
  }


  public long retryDelay() {
    return retryDelay;
  }

  /**
   * The delay between retries.
   */
  public ClientBuilder retryDelay(long retryDelay) {
    this.retryDelay = retryDelay;
    return this;
  }

  public long retryMaxDelay() {
    return retryMaxDelay;
  }

  /**
   * The max backing off delay between retries.
   */
  public ClientBuilder retryMaxDelay(long retryMaxDelay) {
    this.retryMaxDelay = retryMaxDelay;
    return this;
  }

  public ChronoUnit retryChronoUnit() {
    return retryChronoUnit;
  }

  /**
   * the retries period unit.
   */
  public ClientBuilder retryChronoUnit(ChronoUnit retryChronoUnit) {
    this.retryChronoUnit = retryChronoUnit;
    return this;
  }

  public String retryMaxDuration() {
    return retryMaxDuration;
  }

  /**
   * the retries max duration.
   */
  public ClientBuilder retryMaxDuration(String retryMaxDuration) {
    this.retryMaxDuration = retryMaxDuration;
    return this;
  }

  /**
   * build a new Client.
   *
   * @return Client instance.
   * @throws EtcdException if client experiences build error.
   */
  public Client build() {
    checkState(
        !endpoints.isEmpty(),
        "please configure etcd server endpoints before build.");

    return new ClientImpl(this);
  }

  public ClientBuilder copy() {
    try {
      return (ClientBuilder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw EtcdExceptionFactory.toEtcdException(e);
    }
  }
}
