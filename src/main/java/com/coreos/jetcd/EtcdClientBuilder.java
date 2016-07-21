package com.coreos.jetcd;

import io.grpc.internal.ManagedChannelImpl;
import io.grpc.netty.NettyChannelBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * etcd client builder.
 */
public class EtcdClientBuilder {

    /**
     * create a new builder.
     *
     * @return new builder
     */
    public static EtcdClientBuilder newBuilder() {
        return new EtcdClientBuilder();
    }

    private String etcdHost;
    private int etcdPort;

    private EtcdClientBuilder() {

    }

    /**
     * connect to a single etcd server.
     *
     * @param etcdHost etcd host
     * @param etcdPort etcd port
     * @return this instance to chain
     */
    public EtcdClientBuilder connect(String etcdHost, int etcdPort) {
        checkNotNull(etcdHost, "etcdHost should not be null.");
        checkArgument(etcdPort > 0, "etcdHost should greater than zero.");
        this.etcdHost = etcdHost;
        this.etcdPort = etcdPort;
        return this;
    }

    /**
     * build etcd client.
     *
     * @return etcd client
     */
    public EtcdClient build() {
        checkStatusBeforeBuild();

        ManagedChannelImpl channel = buildChannel();
        return new EtcdClient(channel);
    }

    private void checkStatusBeforeBuild() {
        checkState(this.etcdHost != null, "please set etcd host and port");
    }


    /**
     * build etcd native client.
     *
     * @return etcd native client
     */
    public EtcdNativeClient buildNative() {
        ManagedChannelImpl channel = buildChannel();
        return new EtcdNativeClient(channel);
    }

    private ManagedChannelImpl buildChannel() {
        NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder.forAddress(etcdHost, etcdPort);
        return nettyChannelBuilder.usePlaintext(true).build();
    }
}
