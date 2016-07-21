package com.coreos.jetcd;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.List;

public class EtcdClientBuilder {

    public static EtcdClientBuilder newBuilder() {
        return new EtcdClientBuilder();
    }

    private EtcdClientBuilder() {}

    private List<String> endpoints = Lists.newArrayList();

    public EtcdClientBuilder endpoints(List<String> endpoints) {
        this.endpoints = endpoints;
        return this;
    }

    public EtcdClient build() {
        Preconditions.checkNotNull(endpoints);
        Preconditions.checkArgument(!endpoints.isEmpty());
        return new EtcdClient(endpoints);
    }



}
