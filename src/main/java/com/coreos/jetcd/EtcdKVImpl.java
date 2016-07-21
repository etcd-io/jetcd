package com.coreos.jetcd;

import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.TxnResponse;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import java.util.List;

/**
 * Implementation of etcd kv client
 */
public class EtcdKVImpl implements EtcdKV {

    private final List<String> endpoints;

    EtcdKVImpl(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public ListenableFuture<PutResponse> put(ByteString key, ByteString value, PutOption option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<RangeResponse> get(ByteString key, GetOption option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<DeleteRangeResponse> delete(ByteString key, DeleteOption option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<TxnResponse> commit(Txn txn) {
        throw new UnsupportedOperationException();
    }

}
