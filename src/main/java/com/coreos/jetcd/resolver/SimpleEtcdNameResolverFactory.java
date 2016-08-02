package com.coreos.jetcd.resolver;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * A custom name resolver factory which creates etcd name resolver.
 */
public class SimpleEtcdNameResolverFactory extends AbstractEtcdNameResolverFactory {

    private static final String SCHEME = "etcd";
    private static final String NAME = "simple";

    private final URI[] uris;

    public SimpleEtcdNameResolverFactory(final URI... uris) {
        this.uris = uris;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (SCHEME.equals(targetUri.getScheme())) {
            String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
            Preconditions.checkArgument(targetPath.startsWith("/"),
                    "the path component (%s) of the target (%s) must start with '/'", targetPath, targetUri);
            String name = targetPath.substring(1);
            return new SimpleEtcdNameResolver(name, GrpcUtil.SHARED_CHANNEL_EXECUTOR, this.uris);
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

    @Override
    public String name() {
        return NAME;
    }
}
