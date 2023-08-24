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
import io.etcd.jetcd.api.CompactionRequest;
import io.etcd.jetcd.api.VertxKVGrpc;
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
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.support.Requests;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of etcd kv client.
 */
final class KVImpl extends Impl implements KV {
    private final VertxKVGrpc.KVVertxStub stub;
    private final ByteSequence namespace;

    KVImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.stub = connectionManager.newStub(VertxKVGrpc::newVertxStub);
        this.namespace = connectionManager.getNamespace();
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
            () -> stub.put(Requests.mapPutRequest(key, value, option, namespace)),
            response -> new PutResponse(response, namespace),
            Errors::isRetryable);
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
            () -> stub.range(Requests.mapRangeRequest(key, option, namespace)),
            response -> new GetResponse(response, namespace),
            Errors::isRetryable);
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
            () -> stub.deleteRange(Requests.mapDeleteRequest(key, option, namespace)),
            response -> new DeleteResponse(response, namespace),
            Errors::isRetryable);
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev) {
        return this.compact(rev, CompactOption.DEFAULT);
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev, CompactOption option) {
        requireNonNull(option, "option should not be null");

        CompactionRequest request = CompactionRequest.newBuilder()
            .setRevision(rev).setPhysical(option.isPhysical())
            .build();

        return execute(
            () -> stub.compact(request),
            CompactResponse::new,
            Errors::isRetryable);
    }

    @Override
    public Txn txn() {
        return TxnImpl.newTxn(
            request -> execute(
                () -> stub.txn(request),
                response -> new TxnResponse(response, namespace), Errors::isRetryable),
            namespace);
    }
}
