package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.op.Op;
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
                .setValue(value)
                .setLease(option.getLeaseId())
                .setPrevKv(option.getPrevKV())
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
        RangeRequest.Builder builder = RangeRequest.newBuilder()
                .setKey(key)
                .setCountOnly(option.isCountOnly())
                .setLimit(option.getLimit())
                .setRevision(option.getRevision())
                .setKeysOnly(option.isKeysOnly())
                .setSerializable(option.isSerializable())
                .setSortOrder(option.getSortOrder())
                .setSortTarget(option.getSortField());
        if (option.getEndKey().isPresent()) {
            builder.setRangeEnd(option.getEndKey().get());
        }
        return kvStub.range(builder.build());
    }

    // ***************
    // Op.DELETE
    // ***************

    @Override
    public ListenableFuture<DeleteRangeResponse> delete(ByteString key) {
        return delete(key, DeleteOption.DEFAULT);
    }

    @Override
    public ListenableFuture<DeleteRangeResponse> delete(ByteString key, DeleteOption option) {
        DeleteRangeRequest.Builder builder = DeleteRangeRequest.newBuilder()
                .setKey(key)
                .setPrevKv(option.isPrevKV());
        if (option.getEndKey().isPresent()) {
            builder.setRangeEnd(option.getEndKey().get());
        }
        return kvStub.deleteRange(builder.build());
    }

    @Override
    public ListenableFuture<CompactionResponse> compact() {
        return compact(CompactOption.DEFAULT);
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

    @Override
    public ListenableFuture<TxnResponse> commit(Txn txn) {
        return kvStub.txn(txn.toTxnRequest());
    }
}
