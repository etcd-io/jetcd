package com.coreos.jetcd.resolver;

import io.grpc.EquivalentAddressGroup;
import io.grpc.internal.SharedResourceHolder.Resource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * SimpleNameResolver returns pre-configured addresses to the caller.
 */
public class SimpleNameResolver extends AbstractEtcdNameResolver {

  private final EquivalentAddressGroup group;

  public SimpleNameResolver(String name, Resource<ExecutorService> executorResource,
                            List<URI> uris) {
    super(name, executorResource);

    this.group = new EquivalentAddressGroup(
        uris.stream()
            .map(uri -> new InetSocketAddress(uri.getHost(), uri.getPort()))
            .collect(Collectors.toList())
    );
  }

  @Override
  protected EquivalentAddressGroup getAddressGroup() throws Exception {
    return group;
  }
}
