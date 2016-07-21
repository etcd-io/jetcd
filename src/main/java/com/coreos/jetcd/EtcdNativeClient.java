package com.coreos.jetcd;

import com.coreos.jetcd.api.KVGrpc;

import io.grpc.ManagedChannel;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * etcd native client.
 *
 * Native client is just to expose the native etcd clients generated from .proto files.
 *
 * Before we finish the normal etcd client, we can at least use this one.
 */
public class EtcdNativeClient {

    private final ManagedChannel channel;

    EtcdNativeClient(ManagedChannel channel) {
        checkNotNull(channel);
        this.channel = channel;
    }

    /**
     * get new blocking stub for KV service.
     *
     * @return new blocking stub for KV service
     */
    public KVGrpc.KVBlockingStub newKVBlockingStub() {
        return KVGrpc.newBlockingStub(channel);
    }
}
