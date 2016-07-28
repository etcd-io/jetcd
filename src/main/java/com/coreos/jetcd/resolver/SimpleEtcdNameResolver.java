package com.coreos.jetcd.resolver;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.ResolvedServerInfo;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SimpleEtcdNameResolver returns pre-configured addresses to the caller.
 */
public class SimpleEtcdNameResolver extends NameResolver {

    private final String authority;
    private final URI[] uris;

    @GuardedBy("this")
    private boolean shutdown;
    @GuardedBy("this")
    private Listener listener;

    public SimpleEtcdNameResolver(@Nullable String nsAuthority, String name, final URI... uris) {
        URI nameUri = URI.create("//" + name);
        authority = Preconditions.checkNotNull(nameUri.getAuthority(),
                "nameUri (%s) doesn't have an authority", nameUri);
        this.uris = uris;
    }

    @Override
    public final String getServiceAuthority() {
        return authority;
    }

    @Override
    public final synchronized void start(Listener listener) {
        Preconditions.checkState(this.listener == null, "already started");
        this.listener = Preconditions.checkNotNull(listener, "listener");
        List<ResolvedServerInfo> servers = new ArrayList<>(this.uris.length);
        for (URI uri : this.uris) {
            servers.add(new ResolvedServerInfo(new InetSocketAddress(uri.getHost(), uri.getPort()), Attributes.EMPTY));
        }
        this.listener.onUpdate(
                Collections.singletonList(servers), Attributes.EMPTY);
    }

    @Override
    public final synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
    }
}
