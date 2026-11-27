/*
 * Copyright 2016-2021 The jetcd authors
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

package io.etcd.jetcd.impl;

import java.util.concurrent.CompletableFuture;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.kv.CompactResponse;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.op.TxnImpl;
import io.etcd.jetcd.options.CompactOption;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.TxnOption;
import io.etcd.jetcd.support.Requests;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of etcd kv client.
 */
final class KVClient extends AbstractClient implements KV {
    private final ByteSequence namespace;

    KVClient(GrpcService grpcService) {
        super(grpcService);

        this.namespace = grpcService.getNamespace();
    }

    @Override
    public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value) {
        return this.put(key, value, PutOption.DEFAULT);
    }

    @Override
    public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value, PutOption option) {
        requireNonNull(key, "key should not be null");
        requireNonNull(value, "value should not be null");
        requireNonNull(option, "option should not be null");
        return execute(
            io.etcd.jetcd.api.KVGrpcClient.Put,
            Requests.mapPutRequest(key, value, option, namespace),
            responseFactory::newPutResponse,
            option.autoRetry());
    }

    @Override
    public CompletableFuture<GetResponse> get(ByteSequence key) {
        return this.get(key, GetOption.DEFAULT);
    }

    @Override
    public CompletableFuture<GetResponse> get(ByteSequence key, GetOption option) {
        requireNonNull(key, "key should not be null");
        requireNonNull(option, "option should not be null");

        return execute(
            io.etcd.jetcd.api.KVGrpcClient.Range,
            Requests.mapRangeRequest(key, option, namespace),
            responseFactory::newGetResponse,
            true);
    }

    @Override
    public CompletableFuture<DeleteResponse> delete(ByteSequence key) {
        return this.delete(key, DeleteOption.DEFAULT);
    }

    @Override
    public CompletableFuture<DeleteResponse> delete(ByteSequence key, DeleteOption option) {
        requireNonNull(key, "key should not be null");
        requireNonNull(option, "option should not be null");

        return execute(
            io.etcd.jetcd.api.KVGrpcClient.DeleteRange,
            Requests.mapDeleteRequest(key, option, namespace),
            responseFactory::newDeleteResponse,
            option.autoRetry());
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev) {
        return this.compact(rev, CompactOption.DEFAULT);
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev, CompactOption option) {
        requireNonNull(option, "option should not be null");

        io.etcd.jetcd.api.CompactionRequest request = io.etcd.jetcd.api.CompactionRequest.newBuilder()
            .setRevision(rev).setPhysical(option.physical())
            .build();

        return execute(
            io.etcd.jetcd.api.KVGrpcClient.Compact,
            request,
            responseFactory::newCompactResponse,
            true);
    }

    @Override
    public Txn txn() {
        return txn(TxnOption.DEFAULT);
    }

    @Override
    public Txn txn(TxnOption option) {
        return TxnImpl.newTxn(
            request -> execute(
                io.etcd.jetcd.api.KVGrpcClient.Txn,
                request,
                responseFactory::newTxnResponse,
                option.autoRetry()),
            namespace);
    }
}
