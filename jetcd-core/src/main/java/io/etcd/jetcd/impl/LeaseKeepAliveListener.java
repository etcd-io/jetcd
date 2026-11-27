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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.Lease;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;

/**
 * Serializes callbacks for one registration without backpressuring the shared keepalive stream.
 */
final class LeaseKeepAliveListener {
    private static final Logger LOG = LoggerFactory.getLogger(LeaseKeepAliveListener.class);

    private final Vertx vertx;
    private final Lease.Listener listener;
    private final Deque<Runnable> callbacks = new ArrayDeque<>();
    private final CompletableFuture<Void> drained = new CompletableFuture<>();
    private Runnable pendingHeartbeat;
    private Runnable pendingReconnectError;
    private boolean running;
    private boolean terminated;
    private volatile boolean cancelled;

    LeaseKeepAliveListener(Vertx vertx, Lease.Listener listener) {
        this.vertx = vertx;
        this.listener = listener;
    }

    synchronized void onNext(LeaseKeepAliveResponse response) {
        if (pendingHeartbeat != null) {
            callbacks.removeLastOccurrence(pendingHeartbeat);
        }
        pendingHeartbeat = () -> listener.onNext(response);
        enqueue(pendingHeartbeat);
    }

    synchronized void onReconnectError(Throwable error) {
        if (pendingReconnectError != null) {
            callbacks.removeLastOccurrence(pendingReconnectError);
        }
        pendingReconnectError = () -> listener.onError(error);
        enqueue(pendingReconnectError);
    }

    synchronized void terminate(Consumer<Lease.Listener> callback) {
        terminated = true;
        enqueue(() -> callback.accept(listener));
    }

    CompletableFuture<Void> drained() {
        return drained;
    }

    synchronized void cancel() {
        cancelled = true;
        terminated = true;
        callbacks.clear();
        pendingHeartbeat = null;
        pendingReconnectError = null;
        if (!running) {
            drained.complete(null);
        }
    }

    private void enqueue(Runnable callback) {
        callbacks.addLast(callback);
        if (!running) {
            dispatchNext();
        }
    }

    private void dispatchNext() {
        Runnable callback = callbacks.pollFirst();
        running = callback != null;
        if (!running) {
            if (terminated) {
                drained.complete(null);
            }
            return;
        }
        if (callback.equals(pendingHeartbeat)) {
            pendingHeartbeat = null;
        } else if (callback.equals(pendingReconnectError)) {
            pendingReconnectError = null;
        }
        vertx.<Void> executeBlocking(() -> {
            if (!cancelled) {
                callback.run();
            }
            return null;
        }, false).onComplete(this::callbackFinished);
    }

    private void callbackFinished(AsyncResult<Void> outcome) {
        if (outcome.failed()) {
            LOG.warn("Lease keepalive callback failed", outcome.cause());
        }
        synchronized (this) {
            dispatchNext();
        }
    }
}
