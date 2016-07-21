package com.coreos.jetcd;

import io.grpc.ManagedChannel;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * etcd client.
 *
 * TODO: what do we want a client to be ?
 */
public class EtcdClient {

    private final ManagedChannel channel;

    EtcdClient(ManagedChannel channel) {
        checkNotNull(channel);
        this.channel = channel;
    }

}
