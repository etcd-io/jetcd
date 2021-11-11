package io.etcd.jetcd.test;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import io.etcd.jetcd.spi.EndpointTargetResolver;

public class EtcdTestTargetResolver implements EndpointTargetResolver {
    @Override
    public String resolve(String authority, Collection<URI> endpoints) {
        if (endpoints.size() > 1) {
            return String.format(
                "%s://%s/%s",
                "ip",
                authority != null ? authority : "",
                endpoints.stream().map(e -> e.getHost() + ":" + e.getPort()).collect(Collectors.joining(",")));
        }

        URI endpoint = endpoints.iterator().next();

        if (endpoint.getScheme() == null) {
            return String.format(
                "%s://%s/%s",
                EtcdClusterNameResolver.SCHEME,
                authority != null ? authority : "",
                endpoint.getPath());
        }

        if (Objects.equals("cluster", endpoint.getScheme())) {
            return String.format(
                "%s://%s/%s",
                EtcdClusterNameResolver.SCHEME,
                authority != null ? authority : "",
                endpoint.getPath());
        }

        if (Objects.equals("http", endpoint.getScheme())) {
            return String.format(
                "%s://%s/%s",
                "ip",
                authority != null ? authority : "",
                endpoint.getHost() + ":" + endpoint.getPort());
        }

        if (Objects.equals("https", endpoint.getScheme())) {
            return String.format(
                "%s://%s/%s",
                "ip",
                authority != null ? authority : "",
                endpoint.getHost() + ":" + endpoint.getPort());
        }

        throw new IllegalArgumentException("Unsuported scheme: " + endpoint.getScheme());
    }
}
