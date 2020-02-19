/*
 * Copyright 2016-2020 The jetcd authors
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

package io.etcd.jetcd;

import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.Beta;
import io.etcd.jetcd.kv.CompactResponse;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.CompactOption;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.support.CloseableClient;

/**
 * Interface of kv client talking to etcd.
 */
@Beta
public interface KV extends CloseableClient {

    /**
     * put a key-value pair into etcd.
     *
     * @param  key   key in ByteSequence
     * @param  value value in ByteSequence
     * @return       PutResponse
     */
    CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value);

    /**
     * put a key-value pair into etcd with option.
     *
     * @param  key    key in ByteSequence
     * @param  value  value in ByteSequence
     * @param  option PutOption
     * @return        PutResponse
     */
    CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value, PutOption option);

    /**
     * retrieve value for the given key.
     *
     * @param  key key in ByteSequence
     * @return     GetResponse
     */
    CompletableFuture<GetResponse> get(ByteSequence key);

    /**
     * retrieve keys with GetOption.
     *
     * @param  key    key in ByteSequence
     * @param  option GetOption
     * @return        GetResponse
     */
    CompletableFuture<GetResponse> get(ByteSequence key, GetOption option);

    /**
     * delete a key.
     *
     * @param  key key in ByteSequence
     * @return     DeleteResponse
     */
    CompletableFuture<DeleteResponse> delete(ByteSequence key);

    /**
     * delete a key or range with option.
     *
     * @param  key    key in ByteSequence
     * @param  option DeleteOption
     * @return        DeleteResponse
     */
    CompletableFuture<DeleteResponse> delete(ByteSequence key, DeleteOption option);

    /**
     * compact etcd KV history before the given rev.
     *
     * <p>
     * All superseded keys with a revision less than the compaction revision will be removed.
     *
     * @param  rev the revision to compact.
     * @return     CompactResponse
     */
    CompletableFuture<CompactResponse> compact(long rev);

    /**
     * compact etcd KV history before the given rev with option.
     *
     * <p>
     * All superseded keys with a revision less than the compaction revision will be removed.
     *
     * @param  rev    etcd revision
     * @param  option CompactOption
     * @return        CompactResponse
     */
    CompletableFuture<CompactResponse> compact(long rev, CompactOption option);

    /**
     * creates a transaction.
     *
     * @return a Txn
     */
    Txn txn();
}
