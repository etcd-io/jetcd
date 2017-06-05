package com.coreos.jetcd;

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
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.protobuf.ByteString;
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
  public CompletableFuture<PutResponse> put(ByteString key, ByteString value) {
    return put(key, value, PutOption.DEFAULT);
  }

  @Override
  public CompletableFuture<PutResponse> put(ByteString key, ByteString value, PutOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(value, "value should not be null");
    checkNotNull(option, "option should not be null");

    PutRequest request = PutRequest.newBuilder()
        .setKey(key)
        .setValue(value)
        .setLease(option.getLeaseId())
        .setPrevKv(option.getPrevKV())
        .build();

    return FutureConverter.toCompletableFuture(this.stub.put(request));
  }

  // ***************
  // Op.GET
  // ***************

  @Override
  public CompletableFuture<RangeResponse> get(ByteString key) {
    return get(key, GetOption.DEFAULT);
  }

  @Override
  public CompletableFuture<RangeResponse> get(ByteString key, GetOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(option, "option should not be null");

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

    return FutureConverter.toCompletableFuture(this.stub.range(builder.build()));
  }

  // ***************
  // Op.DELETE
  // ***************

  @Override
  public CompletableFuture<DeleteRangeResponse> delete(ByteString key) {
    return delete(key, DeleteOption.DEFAULT);
  }

  @Override
  public CompletableFuture<DeleteRangeResponse> delete(ByteString key, DeleteOption option) {
    checkNotNull(key, "key should not be null");
    checkNotNull(option, "option should not be null");

    DeleteRangeRequest.Builder builder = DeleteRangeRequest.newBuilder()
        .setKey(key)
        .setPrevKv(option.isPrevKV());

    if (option.getEndKey().isPresent()) {
      builder.setRangeEnd(option.getEndKey().get());
    }
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
