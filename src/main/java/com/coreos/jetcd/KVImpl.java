package com.coreos.jetcd;

import static com.coreos.jetcd.Util.byteStringFromByteSequence;
import static com.google.common.base.Preconditions.checkNotNull;

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
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import io.grpc.ManagedChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

/**
 * Implementation of etcd kv client.
 */
class KVImpl implements KV {

  private final KVGrpc.KVFutureStub stub;

  KVImpl(ManagedChannel channel, Optional<String> token) {
    this.stub = ClientUtil.configureStub(KVGrpc.newFutureStub(channel), token);
  }

  // ***************
  // Op.PUT
  // ***************

  @Override
  public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value) {
    return put(key, value, PutOption.DEFAULT);
  }

  @Override
  public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value,
      PutOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(value, "value should not be null");
    checkNotNull(option, "option should not be null");

    PutRequest request = PutRequest.newBuilder()
        .setKey(byteStringFromByteSequence(key))
        .setValue(byteStringFromByteSequence(value))
        .setLease(option.getLeaseId())
        .setPrevKv(option.getPrevKV())
        .build();

    return FutureConverter.toCompletableFuture(this.stub.put(request));
  }

  // ***************
  // Op.GET
  // ***************

  @Override
  public CompletableFuture<RangeResponse> get(ByteSequence key) {
    return get(key, GetOption.DEFAULT);
  }

  @Override
  public CompletableFuture<RangeResponse> get(ByteSequence key, GetOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(option, "option should not be null");

    RangeRequest.Builder builder = RangeRequest.newBuilder()
        .setKey(byteStringFromByteSequence(key))
        .setCountOnly(option.isCountOnly())
        .setLimit(option.getLimit())
        .setRevision(option.getRevision())
        .setKeysOnly(option.isKeysOnly())
        .setSerializable(option.isSerializable())
        .setSortOrder(option.getSortOrder())
        .setSortTarget(option.getSortField());

    option.getEndKey().ifPresent((endKey) ->
        builder.setRangeEnd(byteStringFromByteSequence(endKey)));

    return FutureConverter.toCompletableFuture(this.stub.range(builder.build()));
  }

  // ***************
  // Op.DELETE
  // ***************

  @Override
  public CompletableFuture<DeleteRangeResponse> delete(ByteSequence key) {
    return delete(key, DeleteOption.DEFAULT);
  }

  @Override
  public CompletableFuture<DeleteRangeResponse> delete(ByteSequence key, DeleteOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(option, "option should not be null");

    DeleteRangeRequest.Builder builder = DeleteRangeRequest.newBuilder()
        .setKey(byteStringFromByteSequence(key))
        .setPrevKv(option.isPrevKV());

    option.getEndKey()
        .ifPresent((endKey) -> builder.setRangeEnd(byteStringFromByteSequence(endKey)));

    return FutureConverter.toCompletableFuture(this.stub.deleteRange(builder.build()));
  }

  @Override
  public CompletableFuture<CompactionResponse> compact() {
    return compact(CompactOption.DEFAULT);
  }

  @Override
  public CompletableFuture<CompactionResponse> compact(CompactOption option) {
    checkNotNull(option, "option should not be null");

    CompactionRequest request = CompactionRequest.newBuilder()
        .setRevision(option.getRevision())
        .setPhysical(option.isPhysical())
        .build();

    return FutureConverter.toCompletableFuture(this.stub.compact(request));
  }

  @Override
  public CompletableFuture<TxnResponse> commit(Txn txn) {
    checkNotNull(txn, "txn should not be null");
    return FutureConverter.toCompletableFuture(this.stub.txn(txn.toTxnRequest()));
  }
}
