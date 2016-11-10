package com.coreos.jetcd.resolver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import io.grpc.Attributes;
import io.grpc.ResolvedServerInfo;
import io.grpc.internal.SharedResourceHolder.Resource;

/**
 * SimpleEtcdNameResolver returns pre-configured addresses to the caller.
 */
public class SimpleEtcdNameResolver extends AbstractEtcdNameResolver {

    private final List<ResolvedServerInfo> servers;

    public SimpleEtcdNameResolver(String name, Resource<ExecutorService> executorResource, List<URI> uris) {
        super(name, executorResource);

        this.servers = Collections.unmodifiableList(
            uris.stream()
                .map(uri -> new ResolvedServerInfo(new InetSocketAddress(uri.getHost(), uri.getPort()), Attributes.EMPTY))
                .collect(Collectors.toList())
        );
    }

    @Override
    protected List<ResolvedServerInfo> getServers() {
        return servers;
    }
}
