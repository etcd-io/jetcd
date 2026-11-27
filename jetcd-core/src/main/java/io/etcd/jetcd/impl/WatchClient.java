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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.common.exception.Exceptions;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.options.WatchOption;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedWatchClientException;

/**
 * Watch implementation where each watcher manages its own dedicated gRPC stream.
 */
final class WatchClient extends AbstractClient implements Watch {
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(15);

    private final AtomicBoolean closed;
    private final List<WatchConnection> watchers;
    private final ByteSequence namespace;

    WatchClient(GrpcService grpcService) {
        super(grpcService);
        this.closed = new AtomicBoolean();
        this.watchers = new CopyOnWriteArrayList<>();
        this.namespace = grpcService.getNamespace();
    }

    @Override
    public Watcher watch(ByteSequence key, WatchOption option, Listener listener) {
        return createWatcher(key, option, listener);
    }

    @Override
    public CompletableFuture<Watcher> watchAsync(ByteSequence key, WatchOption option, Listener listener) {
        var watcher = createWatcher(key, option, listener);
        return watcher.getReadyFuture().thenApply(v -> watcher);
    }

    private WatchConnection createWatcher(ByteSequence key, WatchOption option, Listener listener) {
        if (closed.get()) {
            throw newClosedWatchClientException();
        }

        WatchConnection watcher = new WatchConnection(
            key,
            namespace,
            option,
            listener,
            grpc(),
            watchers::remove);

        watchers.add(watcher);
        if (closed.get()) {
            var unused = watcher.closeAsync();
            throw newClosedWatchClientException();
        }
        watcher.start();
        return watcher;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            CompletableFuture.allOf(
                watchers.stream()
                    .map(watcher -> watcher.closeAsync().thenCompose(ignored -> watcher.callbacksClosed()))
                    .toArray(CompletableFuture[]::new))
                .get(CLOSE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Swallow - we're closing
        }
    }

    @Override
    public void requestProgress() {
        if (closed.get()) {
            return;
        }

        for (Watcher watcher : watchers) {
            Exceptions.quietly(watcher::requestProgress);
        }
    }
}
