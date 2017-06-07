package com.coreos.jetcd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.resolver.AbstractEtcdNameResolverFactory;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * ClientBuilder knows how to create an Client instance.
 */
public class ClientBuilder {

  private List<String> endpoints = Lists.newArrayList();
  private ByteSequence name;
  private ByteSequence password;
  private AbstractEtcdNameResolverFactory nameResolverFactory;

  private ClientBuilder() {
  }

  public static ClientBuilder newBuilder() {
    return new ClientBuilder();
  }

  /**
   * gets the endpoints for the builder.
   *
   * @return the list of endpoints configured for the builder
   * @throws IllegalArgumentException if endpoints is empty
   */
  public List<String> endpoints() {
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
  public ClientBuilder endpoints(String... endpoints) {
    checkNotNull(endpoints, "endpoints can't be null");
    checkArgument(endpoints.length > 0, "please configure at lease one endpoint ");

    // TODO: check endpoint is in host:port format
    for (String endpoint : endpoints) {
      checkNotNull(endpoint, "endpoint can't be null");
      final String trimmedEndpoint = endpoint.trim();
      checkArgument(trimmedEndpoint.length() > 0, "invalid endpoint: endpoint=" + endpoint);
      this.endpoints.add(trimmedEndpoint);
    }
    return this;
  }

  public ByteSequence getName() {
    return name;
  }

  /**
   * config etcd auth name.
   *
   * @param name etcd auth name
   * @return this builder
   * @throws NullPointerException if name is null
   */
  public ClientBuilder setName(ByteSequence name) {
    checkNotNull(name, "name can't be null");
    this.name = name;
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
   * @throws NullPointerException if password is null
   */
  public ClientBuilder setPassword(ByteSequence password) {
    checkNotNull(password, "password can't be null");
    this.password = password;
    return this;
  }

  /**
   * config etcd auth password.
   *
   * @param nameResolverFactory etcd nameResolverFactory
   * @return this builder
   * @throws NullPointerException if password is null
   */
  public ClientBuilder setNameResolverFactory(
      AbstractEtcdNameResolverFactory nameResolverFactory) {
    checkNotNull(nameResolverFactory);
    this.nameResolverFactory = nameResolverFactory;
    return this;
  }

  /**
   * get nameResolverFactory for etcd client.
   *
   * @return nameResolverFactory
   */
  public AbstractEtcdNameResolverFactory getNameResolverFactory() {
    return nameResolverFactory;
  }

  /**
   * build a new Client.
   *
   * @return Client instance.
   * @throws ConnectException As network reason, wrong address
   * @throws AuthFailedException This may be caused as wrong username or password
   */
  public Client build() throws ConnectException, AuthFailedException {
    checkState(!endpoints.isEmpty() || nameResolverFactory != null,
        "please configure etcd server endpoints or nameResolverFactory before build.");
    return new Client(null, this);
  }
}
