package com.coreos.jetcd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.coreos.jetcd.internal.impl.ClientImpl;
import com.google.common.collect.Lists;
import io.grpc.LoadBalancer;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * ClientBuilder knows how to create an Client instance.
 */
public class ClientBuilder implements Cloneable {
  private static final List<String> SCHEMES = Arrays.asList("http", "https");
  private List<String> endpoints = Lists.newArrayList();
  private ByteSequence user;
  private ByteSequence password;
  private NameResolver.Factory nameResolverFactory;
  private LoadBalancer.Factory loadBalancerFactory;
  private ManagedChannelBuilder<?> channelBuilder;
  private SslContext sslContext;
  private boolean lazyInitialization = false;

  private ClientBuilder() {
  }

  public static ClientBuilder newBuilder() {
    return new ClientBuilder();
  }

  /**
   * gets the endpoints for the builder.
   *
   * @return the list of endpoints configured for the builder
   */
  public List<String> getEndpoints() {
    return this.endpoints;
  }

  /**
   * configure etcd server endpoints.
   *
   * @param endpoints etcd server endpoints, at least one
   * @return this builder to train
   * @throws NullPointerException if endpoints is null or one of endpoint is null
   * @throws IllegalArgumentException if endpoints is empty or some endpoint is invalid
   */
  public ClientBuilder setEndpoints(String... endpoints) {
    checkNotNull(endpoints, "endpoints can't be null");
    checkArgument(endpoints.length > 0, "please configure at lease one endpoint ");

    for (String endpoint : endpoints) {
      checkNotNull(endpoint, "endpoint can't be null");
      final String trimmedEndpoint = endpoint.trim();
      checkArgument(trimmedEndpoint.length() > 0, "invalid endpoint: endpoint=" + endpoint);
      checkArgument(isValidEndpointFormat(trimmedEndpoint), "invalid format: endpoint=" + endpoint);
      this.endpoints.add(trimmedEndpoint);
    }
    return this;
  }

  public ByteSequence getUser() {
    return user;
  }

  /**
   * config etcd auth user.
   *
   * @param user etcd auth user
   * @return this builder
   * @throws NullPointerException if user is <code>null</code>
   */
  public ClientBuilder setUser(ByteSequence user) {
    checkNotNull(user, "user can't be null");
    this.user = user;
    return this;
  }

  public ByteSequence getPassword() {
    return password;
  }

  /**
   * config etcd auth password.
   *
   * @param password etcd auth password
   * @return this builder
   * @throws NullPointerException if password is <code>null</code>
   */
  public ClientBuilder setPassword(ByteSequence password) {
    checkNotNull(password, "password can't be null");
    this.password = password;
    return this;
  }

  /**
   * config NameResolver factory.
   *
   * @param nameResolverFactory etcd NameResolver.Factory
   * @return this builder
   * @throws NullPointerException if nameResolverFactory is <code>null</code>
   */
  public ClientBuilder setNameResolverFactory(NameResolver.Factory nameResolverFactory) {
    checkNotNull(nameResolverFactory, "nameResolverFactory can't be null");
    this.nameResolverFactory = nameResolverFactory;
    return this;
  }

  /**
   * get NameResolver.Factory for etcd client.
   *
   * @return nameResolverFactory
   */
  public NameResolver.Factory getNameResolverFactory() {
    return nameResolverFactory;
  }

  /**
   * config LoadBalancer factory.
   *
   * @param loadBalancerFactory etcd LoadBalancer.Factory
   * @return this builder
   * @throws NullPointerException if loadBalancerFactory is <code>null</code>
   */
  public ClientBuilder setLoadBalancerFactory(LoadBalancer.Factory loadBalancerFactory) {
    checkNotNull(loadBalancerFactory, "loadBalancerFactory can't be null");
    this.loadBalancerFactory = loadBalancerFactory;
    return this;
  }

  /**
   * get LoadBalancer.Factory for etcd client.
   *
   * @return loadBalancerFactory
   */
  public LoadBalancer.Factory getLoadBalancerFactory() {
    return loadBalancerFactory;
  }

  public ManagedChannelBuilder<?> getChannelBuilder() {
    return channelBuilder;
  }

  public ClientBuilder setChannelBuilder(ManagedChannelBuilder<?> channelBuilder) {
    this.channelBuilder = channelBuilder;
    return this;
  }

  public boolean isLazyInitialization() {
    return lazyInitialization;
  }

  /**
   * Define if the client has to initialize connectivity and authentication on client constructor
   * or delay it to the first call to a client. Default is false.
   *
   * @param lazyInitialization true if the client has to lazily perform connectivity/authentication.
   * @return this builder
   */
  public ClientBuilder setLazyInitialization(boolean lazyInitialization) {
    this.lazyInitialization = lazyInitialization;
    return this;
  }

  public SslContext getSslContext() {
    return sslContext;
  }

  /**
   * SSL/TLS context to use instead of the system default. It must have been configured with {@link
   * GrpcSslContexts}, but options could have been overridden.
   *
   * @param sslContext the ssl context
   * @return this builder
   */
  public ClientBuilder setSslContext(SslContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * build a new Client.
   *
   * @return Client instance.
   * @throws ConnectException As network reason, wrong address
   * @throws AuthFailedException This may be caused as wrong username or password
   */
  public Client build() {
    checkState(!endpoints.isEmpty() || nameResolverFactory != null,
        "please configure etcd server endpoints or nameResolverFactory before build.");
    return new ClientImpl(this);
  }

  public ClientBuilder copy() {
    try {
      return (ClientBuilder)super.clone();
    } catch (CloneNotSupportedException e) {
      throw EtcdExceptionFactory.newEtcdException(e);
    }
  }

  private static boolean isValidEndpointFormat(String endpoint) {
    URL u = null;
    try {
      u = new URL(endpoint);
    } catch (MalformedURLException e) {
      return false;
    }

    if (SCHEMES.stream().noneMatch(u.getProtocol()::equals)) {
      return false;
    }

    // endpoint must contain a port.
    if (u.getPort() == -1) {
      return false;
    }

    // endpoint must not contain a path.
    return u.getPath().isEmpty();
  }
}
