package com.coreos.jetcd.internal.impl;

import static com.coreos.jetcd.options.OptionsUtil.toRangeRequestSortOrder;
import static com.coreos.jetcd.options.OptionsUtil.toRangeRequestSortTarget;
import static com.google.common.base.Preconditions.checkNotNull;

import com.coreos.jetcd.KV;
import com.coreos.jetcd.Txn;
import com.coreos.jetcd.api.CompactionRequest;
import com.coreos.jetcd.api.DeleteRangeRequest;
import com.coreos.jetcd.api.KVGrpc;
import com.coreos.jetcd.api.PutRequest;
import com.coreos.jetcd.api.RangeRequest;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.CompactResponse;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.kv.TxnResponse;
import com.coreos.jetcd.op.TxnImpl;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of etcd kv client.
 */
class KVImpl implements KV {

  private final ClientConnectionManager connectionManager;

  private final KVGrpc.KVFutureStub stub;

  KVImpl(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.stub = connectionManager.newStub(KVGrpc::newFutureStub);
  }

  @Override
  public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value) {
    return this.put(key, value, PutOption.DEFAULT);
  }

  @Override
  public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value,
      PutOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(value, "value should not be null");
    checkNotNull(option, "option should not be null");

    PutRequest request = PutRequest.newBuilder()
        .setKey(Util.byteStringFromByteSequence(key))
        .setValue(Util.byteStringFromByteSequence(value))
        .setLease(option.getLeaseId())
        .setPrevKv(option.getPrevKV())
        .build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.put(request),
        PutResponse::new,
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }

  @Override
  public CompletableFuture<GetResponse> get(ByteSequence key) {
    return this.get(key, GetOption.DEFAULT);
  }

  @Override
  public CompletableFuture<GetResponse> get(ByteSequence key, GetOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(option, "option should not be null");

    RangeRequest.Builder builder = RangeRequest.newBuilder()
        .setKey(Util.byteStringFromByteSequence(key))
        .setCountOnly(option.isCountOnly())
        .setLimit(option.getLimit())
        .setRevision(option.getRevision())
        .setKeysOnly(option.isKeysOnly())
        .setSerializable(option.isSerializable())
        .setSortOrder(toRangeRequestSortOrder(option.getSortOrder()))
        .setSortTarget(toRangeRequestSortTarget(option.getSortField()));

    option.getEndKey().ifPresent((endKey) ->
        builder.setRangeEnd(Util.byteStringFromByteSequence(endKey)));

    RangeRequest request = builder.build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.range(request),
        GetResponse::new,
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }

  @Override
  public CompletableFuture<DeleteResponse> delete(ByteSequence key) {
    return this.delete(key, DeleteOption.DEFAULT);
  }

  @Override
  public CompletableFuture<DeleteResponse> delete(ByteSequence key, DeleteOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(option, "option should not be null");

    DeleteRangeRequest.Builder builder = DeleteRangeRequest.newBuilder()
        .setKey(Util.byteStringFromByteSequence(key))
        .setPrevKv(option.isPrevKV());

    option.getEndKey()
        .ifPresent((endKey) -> builder.setRangeEnd(Util.byteStringFromByteSequence(endKey)));

    DeleteRangeRequest request = builder.build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.deleteRange(request),
        DeleteResponse::new,
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }

  @Override
  public CompletableFuture<CompactResponse> compact(long rev) {
    return this.compact(rev, CompactOption.DEFAULT);
  }

  @Override
  public CompletableFuture<CompactResponse> compact(long rev, CompactOption option) {
    checkNotNull(option, "option should not be null");

    CompactionRequest request = CompactionRequest.newBuilder()
        .setRevision(rev)
        .setPhysical(option.isPhysical())
        .build();

    return Util.toCompletableFutureWithRetry(
        () -> stub.compact(request),
        CompactResponse::new,
        Util::isRetriable,
        connectionManager.getExecutorService()
    );
  }

  public Txn txn() {
    return TxnImpl.newTxn((request) ->
        Util.toCompletableFutureWithRetry(
            () -> stub.txn(request),
            TxnResponse::new,
            Util::isRetriable,
            connectionManager.getExecutorService()
        )
    );
  }
}
