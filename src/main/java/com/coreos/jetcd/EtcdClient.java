package com.coreos.jetcd;

import com.coreos.jetcd.api.KVGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Etcd Client
 */
public class EtcdClient {

    private final ManagedChannelBuilder<?> channelBuilder;
    private final String[] endpoints;
    private final ManagedChannel channel;

    private final EtcdKV kvClient;

    public EtcdClient(ManagedChannelBuilder<?> channelBuilder, EtcdClientBuilder builder) {
        this.endpoints = (String[])builder.endpoints().toArray();
        this.channelBuilder = channelBuilder != null ? channelBuilder : ManagedChannelBuilder.forTarget("");
        this.channelBuilder.nameResolverFactory(null);
        this.channel = this.channelBuilder.build();

        KVGrpc.KVFutureStub kvStub = KVGrpc.newFutureStub(this.channel);

        this.kvClient = newKVClient(kvStub);
    }

        /**
         * create a new KV client.
         *
         * @return new KV client
         */
    public EtcdKV newKVClient(KVGrpc.KVFutureStub stub) {
        return new EtcdKVImpl(stub);
    }
}
