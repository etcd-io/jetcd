package com.coreos.jetcd;

import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.TxnResponse;
import com.coreos.jetcd.op.Txn;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

/**
 * Interface of kv client talking to etcd
 */
@Beta
public interface EtcdKV {

    ListenableFuture<PutResponse> put(ByteString key, ByteString value, PutOption option);

    ListenableFuture<RangeResponse> get(ByteString key, GetOption option);

    ListenableFuture<DeleteRangeResponse> delete(ByteString key, DeleteOption option);

    /**
     * Commit a transaction built from {@link com.coreos.jetcd.op.Txn.Builder}.
     *
     * @param txn txn to commit
     * @return
     */
    ListenableFuture<TxnResponse> commit(Txn txn);

}
