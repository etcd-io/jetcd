/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd.internal.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.etcd.jetcd.options.OptionsUtil.toRangeRequestSortOrder;
import static io.etcd.jetcd.options.OptionsUtil.toRangeRequestSortTarget;

import io.etcd.jetcd.KV;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.api.CompactionRequest;
import io.etcd.jetcd.api.DeleteRangeRequest;
import io.etcd.jetcd.api.KVGrpc;
import io.etcd.jetcd.api.PutRequest;
import io.etcd.jetcd.api.RangeRequest;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.kv.CompactResponse;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.TxnImpl;
import io.etcd.jetcd.options.CompactOption;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
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
