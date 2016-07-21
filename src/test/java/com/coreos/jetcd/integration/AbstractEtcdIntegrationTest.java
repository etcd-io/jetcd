package com.coreos.jetcd.integration;

import com.coreos.jetcd.EtcdClient;
import com.coreos.jetcd.EtcdClientBuilder;
import com.coreos.jetcd.EtcdNativeClient;

public abstract class AbstractEtcdIntegrationTest {

    private static final String etcdHost = "127.0.0.1";
    private static final int etcdPort = 2379;

    private static final EtcdClientBuilder clientBuilder = EtcdClientBuilder.newBuilder().connect(etcdHost, etcdPort);

    /**
     * prepare etcd native cliet.
     *
     * @return etcd native cliet
     */
    protected EtcdNativeClient prepareEtcdNativeClient() {
        return clientBuilder.buildNative();
    }

    /**
     * prepare etcd cliet.
     *
     * @return etcd cliet
     */
    protected EtcdClient prepareEtcdClient() {
        return clientBuilder.build();
    }
}
