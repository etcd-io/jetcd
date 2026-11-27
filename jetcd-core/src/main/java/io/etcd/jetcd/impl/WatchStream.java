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

import io.etcd.jetcd.api.WatchGrpcClient;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.common.exception.Exceptions;
import io.etcd.jetcd.grpc.GrpcService;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.WireFormat;

/**
 * Manages one gRPC stream. The owning watch context handles stream events and read demand.
 */
final class WatchStream {

    interface Handler {
        void onMessage(WatchResponse response);

        void onEnd();

        void onError(Throwable error);

        void onWriteStreamReady();

        void onReadStreamReady();
    }

    private final GrpcService grpcService;
    private final Context context;
    private final Handler handler;

    private GrpcClientRequest<WatchRequest, WatchResponse> request;
    private GrpcClientResponse<WatchRequest, WatchResponse> response;
    private Runnable invalidateToken;
    private WatchResponse.Builder pendingFragments;
    private boolean reading;
    private boolean disconnected;

    WatchStream(GrpcService grpcService, Context context, Handler handler) {
        this.grpcService = grpcService;
        this.context = context;
        this.handler = handler;
    }

    void connect() {
        try {
            grpcService.auth()
                .request(WatchGrpcClient.Watch)
                .onSuccess(result -> context.runOnContext(v -> open(result.request(), result::invalidateToken)))
                .onFailure(this::reportError);
        } catch (RuntimeException error) {
            reportError(error);
        }
    }

    private void open(GrpcClientRequest<WatchRequest, WatchResponse> stream, Runnable invalidate) {
        if (disconnected) {
            stream.cancel();
            return;
        }
        request = stream.format(WireFormat.PROTOBUF);
        invalidateToken = invalidate;
        request.response()
            .onSuccess(result -> {
                result.pause();
                context.runOnContext(v -> attach(result));
            })
            .onFailure(this::reportError);
        handler.onWriteStreamReady();
    }

    private void attach(GrpcClientResponse<WatchRequest, WatchResponse> stream) {
        if (disconnected) {
            return;
        }
        GrpcStatus status = stream.status();
        if (status != null && status != GrpcStatus.OK) {
            handler.onError(new InvalidStatusException(GrpcStatus.OK, status));
            return;
        }

        response = stream;
        stream.handler(message -> context.runOnContext(v -> receive(message)));
        stream.exceptionHandler(this::reportError);
        stream.endHandler(v -> context.runOnContext(ignored -> {
            if (!disconnected) {
                handler.onEnd();
            }
        }));
        handler.onReadStreamReady();
    }

    void requestNext() {
        if (response != null && !reading && !disconnected) {
            reading = true;
            response.fetch(1);
        }
    }

    private void receive(WatchResponse message) {
        if (disconnected) {
            return;
        }
        reading = false;
        WatchResponse complete = assemble(message);
        if (complete == null) {
            requestNext();
        } else {
            handler.onMessage(complete);
        }
    }

    void send(WatchRequest message) {
        if (request != null && !disconnected) {
            request.write(message).onFailure(this::reportError);
        }
    }

    void invalidateToken() {
        invalidateToken.run();
    }

    private void reportError(Throwable error) {
        context.runOnContext(v -> {
            if (!disconnected) {
                handler.onError(error);
            }
        });
    }

    Future<Void> disconnect() {
        disconnected = true;
        pendingFragments = null;
        var read = response;
        response = null;
        if (read != null) {
            Exceptions.quietly(() -> read.handler(null).endHandler(null).exceptionHandler(null));
        }
        var write = request;
        request = null;
        if (write == null) {
            return Future.succeededFuture();
        }
        try {
            write.cancel();
            return write.cancellation();
        } catch (RuntimeException error) {
            return Future.failedFuture(error);
        }
    }

    private WatchResponse assemble(WatchResponse message) {
        if (message.getCreated() || message.getCanceled()) {
            pendingFragments = null;
            return message;
        }
        if (pendingFragments == null) {
            if (message.getFragment()) {
                pendingFragments = message.toBuilder();
                return null;
            }
            return message;
        }
        if (message.getEventsCount() == 0) {
            return null;
        }
        pendingFragments.addAllEvents(message.getEventsList());
        if (message.getFragment()) {
            return null;
        }
        WatchResponse complete = pendingFragments.setFragment(false).build();
        pendingFragments = null;
        return complete;
    }
}
