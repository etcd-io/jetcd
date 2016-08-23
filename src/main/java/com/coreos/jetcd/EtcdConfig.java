package com.coreos.jetcd;

import com.coreos.jetcd.resolver.AbstractEtcdNameResolverFactory;
import com.google.protobuf.ByteString;

import java.util.List;


public class EtcdConfig {
    public final String[] endpoints;
    public final ByteString name;
    public final ByteString password;
    public final AbstractEtcdNameResolverFactory nameResolverFactory;

    public EtcdConfig(List<String> endpoints, ByteString name, ByteString password, AbstractEtcdNameResolverFactory nameResolverFactory) {
        this.endpoints = new String[endpoints.size()];
        endpoints.toArray(this.endpoints);
        this.name = name;
        this.password = password;
        this.nameResolverFactory = nameResolverFactory;
    }

}
