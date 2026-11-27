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
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.Exceptions;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.Responses;
import io.etcd.jetcd.watch.WatchResponse;
import io.etcd.jetcd.watch.WatchState;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.grpc.common.GrpcStatus;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedWatchClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

/**
 * Individual watcher that owns its dedicated gRPC stream.
 * Uses WatchStateMachine for lifecycle management, WatchStream for gRPC stream handling,
 * and WatchReconnectionManager for retry logic with exponential backoff.
 */
final class WatchConnection implements Watch.Watcher, WatchStream.Handler {
    private final ByteSequence key;
    private final ByteSequence namespace;
    private final WatchOption option;
    private final Watch.Listener listener;
    private final GrpcService grpcService;
    private final CompletableFuture<Context> ownerContext;
    private final Consumer<WatchConnection> onClose;
    private final WatchResponseProcessor responseProcessor;
    private final WatchStateMachine stateMachine;
    private final Responses.Namespaced responseFactory;
    private final CompletableFuture<Void> readyFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> callbacksClosed = new CompletableFuture<>();

    // Owned by context.
    private Context context;
    private WatchReconnectionManager reconnectionManager;
    private final Queue<Notification> notifications = new ArrayDeque<>();
    private WatchStream watchStream;
    private boolean callbackRunning;
    private boolean reconnectReady;
    private boolean pendingProgressRequest;
    private long revision;

    WatchConnection(
        ByteSequence key,
        ByteSequence namespace,
        WatchOption option,
        Watch.Listener listener,
        GrpcService grpcService,
        Consumer<WatchConnection> onClose) {

        this.key = key;
        this.namespace = namespace;
        this.option = option;
        this.listener = listener;
        this.revision = option.revision();
        this.grpcService = grpcService;
        var vertx = grpcService.vertx();
        Context callerContext = vertx.getOrCreateContext();
        // A worker caller can share the same pool as the listener. Use an event-loop context for the watch state.
        this.ownerContext = callerContext.isEventLoopContext()
            ? CompletableFuture.completedFuture(callerContext)
            : CompletableFuture.supplyAsync(vertx::getOrCreateContext);
        this.onClose = onClose;
        this.responseProcessor = new WatchResponseProcessor(option);
        this.responseFactory = Responses.namespaced(namespace);
        this.stateMachine = new WatchStateMachine(new WatchStateMachine.Handler() {
            @Override
            public void onConnect() {
                handleConnect();
            }

            @Override
            public void onSubscribe() {
                handleSubscribe();
            }

            @Override
            public void onReady() {
                handleReady();
            }

            @Override
            public void onReconnect() {
                handleReconnect();
            }

            @Override
            public void onClose() {
                handleClose();
            }

            @Override
            public void onError(Throwable error) {
                handleError(error);
            }

            @Override
            public void onStateChange(WatchStateMachine.State oldState, WatchStateMachine.State newState) {
                handleStateChange(oldState, newState);
            }
        });
    }

    void start() {
        runOnOwner(() -> {
            if (!stateMachine.isClosed()) {
                stateMachine.start();
            }
        });
    }

    @Override
    public boolean isClosed() {
        return stateMachine.isClosed();
    }

    CompletableFuture<Void> getReadyFuture() {
        return readyFuture;
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        runOnOwner(stateMachine::close);
        return closeFuture;
    }

    CompletableFuture<Void> callbacksClosed() {
        return callbacksClosed;
    }

    @Override
    public void requestProgress() {
        if (stateMachine.isClosed()) {
            return;
        }

        runOnOwner(() -> {
            if (stateMachine.isClosed()) {
                return;
            }
            if (!stateMachine.isWatching()) {
                pendingProgressRequest = true;
                return;
            }

            WatchStream stream = watchStream;
            if (stream == null) {
                pendingProgressRequest = true;
                return;
            }

            stream.send(WatchRequestFactory.progressRequest());
            pendingProgressRequest = false;
        });
    }

    private void runOnOwner(Runnable action) {
        var scheduled = ownerContext.thenAccept(owner -> owner.runOnContext(ignored -> {
            if (context == null) {
                context = owner;
                reconnectionManager = new WatchReconnectionManager(
                    context, option, retry -> notifyListener(() -> listener.onRetry(retry)), stateMachine);
            }
            action.run();
        }));
        scheduled.exceptionally(error -> {
            readyFuture.completeExceptionally(error);
            closeFuture.completeExceptionally(error);
            callbacksClosed.completeExceptionally(error);
            onClose.accept(this);
            return null;
        });
    }

    // State machine handlers

    private void handleConnect() {
        watchStream = new WatchStream(grpcService, context, this);
        watchStream.connect();
    }

    private void handleSubscribe() {
        WatchStream stream = watchStream;
        if (stream != null) {
            stream.send(WatchRequestFactory.createRequest(key, namespace, option, revision));
        }
    }

    private void handleReady() {
        readyFuture.complete(null);

        // Send pending progress request if one was queued
        if (pendingProgressRequest) {
            pendingProgressRequest = false;
            WatchStream stream = watchStream;
            if (stream != null) {
                stream.send(WatchRequestFactory.progressRequest());
            }
        }
    }

    private void handleReconnect() {
        reconnectionManager.attemptReconnection(
            this::disconnect,
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    reconnectReady = true;
                    advance();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    stateMachine.reconnectFailed(error);
                }
            });
    }

    private void handleClose() {
        reconnectionManager.cancelReconnection();

        readyFuture.completeExceptionally(newClosedWatchClientException());
        disconnect().onComplete(result -> context.runOnContext(ignored -> {
            notifyListener(listener::onCompleted, () -> {
                Exceptions.quietly(() -> onClose.accept(this));
                callbacksClosed.complete(null);
            });
            closeFuture.complete(null);
        }));
    }

    private void handleError(Throwable error) {
        EtcdException etcdException = toEtcdException(error);
        notifyListener(() -> listener.onError(etcdException));
        readyFuture.completeExceptionally(error);
    }

    private void handleStateChange(WatchStateMachine.State oldState, WatchStateMachine.State newState) {
        WatchState oldPublicState = toPublicState(oldState);
        WatchState newPublicState = toPublicState(newState);
        notifyListener(() -> listener.onStateChange(oldPublicState, newPublicState));
    }

    private static WatchState toPublicState(WatchStateMachine.State state) {
        return switch (state) {
            case WatchStateMachine.State.Connecting() -> WatchState.CONNECTING;
            case WatchStateMachine.State.Subscribing() -> WatchState.SUBSCRIBING;
            case WatchStateMachine.State.Watching() -> WatchState.WATCHING;
            case WatchStateMachine.State.Reconnecting() -> WatchState.RECONNECTING;
            case WatchStateMachine.State.Closed() -> WatchState.CLOSED;
        };
    }

    // WatchStream.Handler implementation

    @Override
    public void onMessage(io.etcd.jetcd.api.WatchResponse response) {
        if (stateMachine.isClosed()) {
            return;
        }

        switch (responseProcessor.process(response)) {
            case WatchResponseProcessor.Result.AuthError() -> {
                watchStream.invalidateToken();
                stateMachine.streamError(toEtcdException(GrpcStatus.CANCELLED));
            }
            case WatchResponseProcessor.Result.Created(long rev, boolean shouldNotify) -> {
                // Only a live watch needs an initial revision. Replay can still follow this acknowledgment.
                if (revision == 0) {
                    revision = rev;
                }
                stateMachine.watchCreated();
                if (shouldNotify) {
                    WatchResponse created = responseFactory.newWatchResponse(response);
                    notifyListener(() -> listener.onNext(created));
                }
            }
            case WatchResponseProcessor.Result.Canceled(Throwable error) -> {
                stateMachine.watchCanceled(error);
            }
            case WatchResponseProcessor.Result.Progress progress -> deliver(response, Math.max(revision, progress.revision()));
            case WatchResponseProcessor.Result.Events(long newRevision) -> deliver(response, newRevision);
            case WatchResponseProcessor.Result.Ignored() -> {
                // No action needed
            }
        }
        advance();
    }

    @Override
    public void onEnd() {
        stateMachine.streamEnded();
    }

    @Override
    public void onError(Throwable error) {
        stateMachine.streamError(error);
    }

    @Override
    public void onWriteStreamReady() {
        stateMachine.streamReady();
    }

    @Override
    public void onReadStreamReady() {
        advance();
    }

    // Private helpers

    private Future<Void> disconnect() {
        pendingProgressRequest = false;

        WatchStream ws = watchStream;
        watchStream = null;

        return ws == null ? Future.succeededFuture() : ws.disconnect();
    }

    private void deliver(io.etcd.jetcd.api.WatchResponse response, long nextRevision) {
        WatchResponse value = responseFactory.newWatchResponse(response);
        notifyListener(() -> listener.onNext(value), () -> revision = nextRevision);
    }

    private void notifyListener(Runnable callback) {
        notifyListener(callback, () -> {
        });
    }

    private void notifyListener(Runnable callback, Runnable completion) {
        notifications.add(new Notification(callback, completion));
        advance();
    }

    private void advance() {
        if (callbackRunning) {
            return;
        }
        Notification notification = notifications.poll();
        if (notification != null) {
            callbackRunning = true;
            context.executeBlocking(() -> {
                Exceptions.quietly(notification.callback());
                return null;
            }, false).onComplete(ignored -> {
                notification.completion().run();
                callbackRunning = false;
                advance();
            });
        } else if (!stateMachine.isClosed()) {
            if (reconnectReady) {
                reconnectReady = false;
                stateMachine.reconnectSucceeded();
            } else if (watchStream != null && !(stateMachine.currentState() instanceof WatchStateMachine.State.Reconnecting)) {
                watchStream.requestNext();
            }
        }
    }

    private record Notification(Runnable callback, Runnable completion) {
    }
}
