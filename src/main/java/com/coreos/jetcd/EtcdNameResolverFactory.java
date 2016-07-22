package com.coreos.jetcd;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.net.URI;

/**
 * A custom name resolver factory which creates etcd name resolver.
 */
public class EtcdNameResolverFactory extends NameResolver.Factory {

    private static final String SCHEME = "etcd";

    private final String[] address;

    public EtcdNameResolverFactory(String... addresses) {
        this.address = addresses;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (SCHEME.equals(targetUri.getScheme())) {

        } else {
            return null;
        }
        return null;
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

    private static class EtcdNameResolver extends NameResolver {

        private final Object lock = new Object();

        private final String[] servers;

        @GuardedBy("lock")
        private boolean shutdown;
        @GuardedBy("lock")
        private Listener listener;

        public EtcdNameResolver(String... servers) {
            this.servers = servers;
        }

        @Override
        public String getServiceAuthority() {
            return "";
        }

        @Override
        public void start(Listener listener) {
            synchronized (lock) {
                Preconditions.checkState(this.listener == null, "already started");
                this.listener = Preconditions.checkNotNull(listener, "listener");
            }
        }

        @Override
        public void shutdown() {
            synchronized (lock) {
                if (shutdown) {
                    return;
                }
                shutdown = true;
            }
        }
    }
}
