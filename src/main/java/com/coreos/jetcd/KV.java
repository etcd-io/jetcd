package com.coreos.jetcd;

import com.coreos.jetcd.api.CompactionResponse;
import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.TxnResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.annotations.Beta;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of kv client talking to etcd.
 */
@Beta
public interface KV {

  /**
   * put a key-value pair into etcd.
   *
   * @param key key in ByteSequence
   * @param value value in ByteSequence
   * @return PutResponse
   */
  CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value);

  /**
   * put a key-value pair into etcd with option.
   *
   * @param key key in ByteSequence
   * @param value value in ByteSequence
   * @param option PutOption
   * @return PutResponse
   */
  CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value, PutOption option);

  /**
   * retrieve value for the given key.
   *
   * @param key key in ByteSequence
   * @return RangeResponse
   */
  CompletableFuture<RangeResponse> get(ByteSequence key);

  /**
   * retrieve keys with GetOption.
   *
   * @param key key in ByteSequence
   * @param option GetOption
   * @return RangeResponse
   */
  CompletableFuture<RangeResponse> get(ByteSequence key, GetOption option);

  /**
   * delete a key.
   *
   * @param key key in ByteSequence
   * @return DeleteRangeResponse
   */
  CompletableFuture<DeleteRangeResponse> delete(ByteSequence key);

  /**
   * delete a key or range with option.
   *
   * @param key key in ByteSequence
   * @param option DeleteOption
   * @return DeleteRangeResponse
   */
  CompletableFuture<DeleteRangeResponse> delete(ByteSequence key, DeleteOption option);

  /**
   * compact etcd KV history before the given rev.
   *
   *<p>All superseded keys with a revision less than the compaction revision will be removed.
   *
   * @param rev the revision to compact.
   * @return CompactionResponse
   */
  CompletableFuture<CompactionResponse> compact(long rev);

  /**
   * compact etcd KV history before the given rev with option.
   *
   *<p>All superseded keys with a revision less than the compaction revision will be removed.
   *
   * @param rev etcd revision
   * @param option CompactOption
   * @return CompactionResponse
   */
  CompletableFuture<CompactionResponse> compact(long rev, CompactOption option);

  /**
   * Commit a transaction built from {@link com.coreos.jetcd.op.Txn.Builder}.
   *
   * @param txn txn to commit
   */
  CompletableFuture<TxnResponse> commit(Txn txn);
}

