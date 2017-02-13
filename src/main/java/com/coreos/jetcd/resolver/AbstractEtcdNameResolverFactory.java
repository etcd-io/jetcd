package com.coreos.jetcd.resolver;

import io.grpc.NameResolver;

/**
 * The abstract etcd name resolver factory. Name resolver factory is responsible for
 * creating specific name resolver which provides addresses for {@link io.grpc.LoadBalancer}.
 * Currently we support two kinds name resolver: simple, dns.
 */
public abstract class AbstractEtcdNameResolverFactory extends NameResolver.Factory {

  // Gets name of name resolver, only "simple" and "dns" are validate.
  public abstract String name();
}
