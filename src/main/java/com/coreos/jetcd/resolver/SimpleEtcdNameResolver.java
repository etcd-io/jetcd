package com.coreos.jetcd.resolver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import io.grpc.Attributes;
import io.grpc.ResolvedServerInfo;
import io.grpc.internal.SharedResourceHolder.Resource;

/**
 * SimpleEtcdNameResolver returns pre-configured addresses to the caller.
 */
public class SimpleEtcdNameResolver extends AbstractEtcdNameResolver {

    private final URI[] uris;

    public SimpleEtcdNameResolver(String name, Resource<ExecutorService> executorResource, final URI... uris) {
        super(name, executorResource);
        this.uris = uris;
    }

    @Override
    protected List<ResolvedServerInfo> getServers() {
        List<ResolvedServerInfo> servers = new ArrayList<>(uris.length);
        for (URI uri : uris) {
            servers.add(new ResolvedServerInfo(new InetSocketAddress(uri.getHost(), uri.getPort()), Attributes.EMPTY));
        }
        return servers;
    }
}
