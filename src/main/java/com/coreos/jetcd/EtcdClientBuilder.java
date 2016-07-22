package com.coreos.jetcd;

import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * ClientBuilder knows how to create an EtcdClient instance.
 */
public class EtcdClientBuilder {

    private List<String> endpoints = Lists.newArrayList();

    private EtcdClientBuilder() {
    }

    public static EtcdClientBuilder newBuilder() {
        return new EtcdClientBuilder();
    }

    public List<String> endpoints() {
        return this.endpoints;
    }

    /**
     * configure etcd server endpoints.
     *
     * @param endpoints etcd server endpoints, at least one
     * @return this builder to train
     * @throws NullPointerException if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if endpoints is empty or some endpoint is invalid
     */
    public EtcdClientBuilder endpoints(String ... endpoints) {
        checkNotNull(endpoints, "endpoints can't be null");
        checkArgument(endpoints.length > 0, "please configure at lease one endpoint ");

        for(String endpoint : endpoints) {
            checkNotNull(endpoint, "endpoint can't be null");
            final String trimmedEndpoint = endpoint.trim();
            checkArgument(trimmedEndpoint.length() > 0, "invalid endpoint: endpoint=" + endpoint);
            this.endpoints.add(trimmedEndpoint);
        }
        return this;
    }


    /**
     * build a new EtcdClient.
     *
     * @throws IllegalStateException if etcd serve endpoints are not set
     * @return EtcdClient instance.
     */
    public EtcdClient build() {
        checkState(!endpoints.isEmpty(), "please configure ectd serve endpoints by method endpoints() before build.");
        return new EtcdClient(null, this);
    }



}
