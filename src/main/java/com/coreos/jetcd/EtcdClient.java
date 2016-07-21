package com.coreos.jetcd;

import java.util.List;

/**
 * Etcd Client
 */
public class EtcdClient {

    private final List<String> endpoints;

    EtcdClient(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public EtcdKV newKVClient() {
        return new EtcdKVImpl(endpoints);
    }

}
