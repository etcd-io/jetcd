package com.coreos.jetcd.resolver;

import io.grpc.EquivalentAddressGroup;
import java.net.URI;
import java.util.List;

public interface URIResolver {
  /**
   * Determine the priority for the resolver. As resolvers are loaded using the ServiceLoader the
   * order is not deterministic and it may change depending on the operating system.
   *
   * @return the priority
   */
  default int priority() {
    return Integer.MAX_VALUE;
  }

  /**
   * Check if this resolver supports the given {@link URI}.
   *
   * @param uri the uri
   * @return true if the resolver supports the URI
   */
  boolean supports(URI uri);

  /**
   * Resolve the uri.
   *
   * @param uri the uri
   * @return a list of {@link EquivalentAddressGroup}
   */
  List<EquivalentAddressGroup> resolve(URI uri);
}
