package com.coreos.jetcd.resolver;

import java.net.URI;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;

/**
 * A custom name resolver factory which creates etcd name resolver.
 */
public class DnsSrvNameResolverFactory extends AbstractEtcdNameResolverFactory {

    private static final String SCHEME = "dns+srv";
    private static final String NAME = "dns+srv";

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (SCHEME.equals(targetUri.getScheme())) {
            String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");

            Preconditions.checkArgument(
                targetPath.startsWith("/"),
                "the path component (%s) of the target (%s) must start with '/'", targetPath, targetUri
            );

            String name = targetPath.substring(1);
            if (!name.startsWith("_etcd-server._tcp.")) {
                name = "_etcd-server._tcp." + name;
            }

            return new DnsSrvNameResolver(name, GrpcUtil.SHARED_CHANNEL_EXECUTOR);
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
