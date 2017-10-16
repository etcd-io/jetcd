/**
 * Copyright 2017 The jetcd authors
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

package com.coreos.jetcd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.coreos.jetcd.internal.impl.ClientImpl;
import com.coreos.jetcd.resolver.URIResolverLoader;
import io.grpc.LoadBalancer;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ClientBuilder knows how to create an Client instance.
 */
public final class ClientBuilder implements Cloneable {

  private Set<String> endpoints = new HashSet<>();
  private ByteSequence user;
  private ByteSequence password;
  private LoadBalancer.Factory loadBalancerFactory;
  private SslContext sslContext;
  private boolean lazyInitialization = false;
  private String authority;
  private URIResolverLoader uriResolverLoader;

  ClientBuilder() {
  }

  /**
   * gets the endpoints for the builder.
   *
   * @return the list of endpoints configured for the builder
   */
  public Collection<String> endpoints() {
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
  public ClientBuilder endpoints(Collection<String> endpoints) {
    checkNotNull(endpoints, "endpoints can't be null");

    for (String endpoint : endpoints) {
      checkNotNull(endpoint, "endpoint can't be null");
      final String trimmedEndpoint = endpoint.trim();
      checkArgument(trimmedEndpoint.length() > 0, "invalid endpoint: endpoint=" + endpoint);
      this.endpoints.add(trimmedEndpoint);
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
  public ClientBuilder endpoints(String... endpoints) {
    checkNotNull(endpoints, "endpoints can't be null");

    return endpoints(Arrays.asList(endpoints));
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

  /**
   * config LoadBalancer factory.
   *
   * @param loadBalancerFactory etcd LoadBalancer.Factory
   * @return this builder
   * @throws NullPointerException if loadBalancerFactory is <code>null</code>
   */
  public ClientBuilder loadBalancerFactory(LoadBalancer.Factory loadBalancerFactory) {
    checkNotNull(loadBalancerFactory, "loadBalancerFactory can't be null");
    this.loadBalancerFactory = loadBalancerFactory;
    return this;
  }

  /**
   * get LoadBalancer.Factory for etcd client.
   *
   * @return loadBalancerFactory
   */
  public LoadBalancer.Factory loadBalancerFactory() {
    return loadBalancerFactory;
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

  /**
   * build a new Client.
   *
   * @return Client instance.
   * @throws com.coreos.jetcd.exception.EtcdException if client experiences build error.
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
