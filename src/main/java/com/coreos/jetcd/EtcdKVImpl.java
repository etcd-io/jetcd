package com.coreos.jetcd;

import com.coreos.jetcd.api.CompactionRequest;
import com.coreos.jetcd.api.CompactionResponse;
import com.coreos.jetcd.api.DeleteRangeRequest;
import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.KVGrpc;
import com.coreos.jetcd.api.PutRequest;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.api.RangeRequest;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.TxnResponse;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;


/**
 * Implementation of etcd kv client
 */
class EtcdKVImpl implements EtcdKV {

    private final KVGrpc.KVFutureStub kvStub;

    EtcdKVImpl(final KVGrpc.KVFutureStub kvStub) {
        this.kvStub = kvStub;
    }


    // ***************
    // Op.PUT
    // ***************

    @Override
    public ListenableFuture<PutResponse> put(ByteString key, ByteString value) {
        return put(key, value, PutOption.DEFAULT);
    }

    @Override
    public ListenableFuture<PutResponse> put(ByteString key, ByteString value, PutOption option) {
        PutRequest request =
            PutRequest.newBuilder()
                .setKey(key)
                .setLease(option.getLeaseId())
                .build();
        return kvStub.put(request);
    }

    // ***************
    // Op.GET
    // ***************

    @Override
    public ListenableFuture<RangeResponse> get(ByteString key) {
        return get(key, GetOption.DEFAULT);
    }

    @Override
    public ListenableFuture<RangeResponse> get(ByteString key, GetOption option) {
        RangeRequest request =
            RangeRequest.newBuilder()
                .setKey(key)
                .setCountOnly(option.isCountOnly())
                .setKeysOnly(option.isKeysOnly())
                .setLimit(option.getLimit())
                .setSerializable(option.isSerializable())
                .setSortOrder(option.getSortOrder())
                .setSortTarget(option.getSortField())
                .build();
        return kvStub.range(request);
    }

    // ***************
    // Op.DEETE
    // ***************

    @Override
    public ListenableFuture<DeleteRangeResponse> delete(ByteString key) {
        return delete(key, DeleteOption.DEFAULT);
    }

    @Override
    public ListenableFuture<DeleteRangeResponse> delete(ByteString key, DeleteOption option) {
        DeleteRangeRequest request =
            DeleteRangeRequest.newBuilder()
                .setKey(key)
                .setPrevKv(option.isPrevKV())
                .setRangeEnd(option.getEndKey().get())
                .build();
        return kvStub.deleteRange(request);
    }

    @Override
    public ListenableFuture<TxnResponse> commit(Txn txn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CompactionResponse> compact(CompactOption option) {
        CompactionRequest request =
            CompactionRequest.newBuilder()
                .setRevision(option.getRevision())
                .setPhysical(option.isPhysical())
                .build();
        return kvStub.compact(request);
    }
}
