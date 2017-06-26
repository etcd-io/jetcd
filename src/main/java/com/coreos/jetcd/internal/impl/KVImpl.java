package com.coreos.jetcd.internal.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.coreos.jetcd.KV;
import com.coreos.jetcd.api.CompactionRequest;
import com.coreos.jetcd.api.DeleteRangeRequest;
import com.coreos.jetcd.api.KVGrpc;
import com.coreos.jetcd.api.PutRequest;
import com.coreos.jetcd.api.RangeRequest;
import com.coreos.jetcd.api.TxnResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.CompactResponse;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import io.grpc.ManagedChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

/**
 * Implementation of etcd kv client.
 */
class KVImpl implements KV {

  private final KVGrpc.KVFutureStub stub;

  private final ExecutorService executorService;

  KVImpl(ClientImpl c) {
    this(c.getChannel(), c.getToken(), c.getExecutorService());
  }

  KVImpl(ManagedChannel channel, Optional<String> token, ExecutorService executorService) {
    this.stub = ClientUtil.configureStub(KVGrpc.newFutureStub(channel), token);
    this.executorService = executorService;
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

    return Util.listenableToCompletableFuture(this.stub.put(request), Util::toPutResponse,
        this.executorService);
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
        .setSortOrder(option.getSortOrder())
        .setSortTarget(option.getSortField());

    option.getEndKey().ifPresent((endKey) ->
        builder.setRangeEnd(Util.byteStringFromByteSequence(endKey)));

    return Util.listenableToCompletableFuture(this.stub.range(builder.build()), Util::toGetResponse,
        this.executorService);
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

    return Util.listenableToCompletableFuture(this.stub.deleteRange(builder.build()),
        Util::toDeleteResponse, this.executorService);
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

    return Util.listenableToCompletableFuture(this.stub.compact(request), Util::toCompactResponse,
        this.executorService);
  }

  // TODO: remove type dependency api TxnResponse.
  @Override
  public CompletableFuture<TxnResponse> commit(Txn txn) {
    checkNotNull(txn, "txn should not be null");
    return FutureConverter.toCompletableFuture(this.stub.txn(txn.toTxnRequest()));
  }
}
