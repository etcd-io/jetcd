package com.coreos.jetcd;

import com.coreos.jetcd.api.CompactionResponse;
import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.TxnResponse;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.annotations.Beta;
import com.google.protobuf.ByteString;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of kv client talking to etcd.
 */
@Beta
public interface KV {

  // ***************
  // Op.PUT
  // ***************

  CompletableFuture<PutResponse> put(ByteString key, ByteString value);

  CompletableFuture<PutResponse> put(ByteString key, ByteString value, PutOption option);

  // ***************
  // Op.GET
  // ***************

  CompletableFuture<RangeResponse> get(ByteString key);

  CompletableFuture<RangeResponse> get(ByteString key, GetOption option);

  // ***************
  // Op.DELETE
  // ***************

  CompletableFuture<DeleteRangeResponse> delete(ByteString key);

  CompletableFuture<DeleteRangeResponse> delete(ByteString key, DeleteOption option);

  // ***************
  // Op.COMPACT
  // ***************

  CompletableFuture<CompactionResponse> compact();

  CompletableFuture<CompactionResponse> compact(CompactOption option);

  /**
   * Commit a transaction built from {@link com.coreos.jetcd.op.Txn.Builder}.
   *
   * @param txn txn to commit
   */
  CompletableFuture<TxnResponse> commit(Txn txn);
}
