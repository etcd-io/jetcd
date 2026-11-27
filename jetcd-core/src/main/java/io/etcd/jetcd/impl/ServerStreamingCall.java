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

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.common.exception.Exceptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.GrpcClientRequestProvider;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.WireFormat;

/**
 * Delivers a server stream on workers while letting each callback control demand for the next message.
 */
final class ServerStreamingCall<Resp> {
    private static final Logger LOG = LoggerFactory.getLogger(ServerStreamingCall.class);

    @FunctionalInterface
    interface MessageHandler<T> {
        void handle(T message) throws Exception;
    }

    private final GrpcClientResponse<?, Resp> response;
    private final MessageHandler<Resp> handler;
    private final Future<Void> end;
    private final Promise<Void> result = Promise.promise();
    private boolean handlingMessage;

    private ServerStreamingCall(GrpcClientResponse<?, Resp> response, MessageHandler<Resp> handler) {
        this.response = response.pause();
        this.handler = handler;
        this.end = response.end();
    }

    static <Req, Resp> Future<Void> consume(GrpcClientRequestProvider requests, ServiceMethod<Resp, Req> method,
        Req request, MessageHandler<Resp> handler) {
        return requests.request(method)
            .compose(stream -> stream.format(WireFormat.PROTOBUF).end(request)
                .compose(ignored -> stream.response()
                    .compose(response -> new ServerStreamingCall<>(response, handler).start())));
    }

    static void notifyListener(Vertx vertx, Future<Void> stream, Consumer<Throwable> onError, Runnable onCompleted) {
        stream.transform(outcome -> vertx.<Void> executeBlocking(() -> {
            if (outcome.succeeded()) {
                onCompleted.run();
            } else {
                onError.accept(EtcdExceptionFactory.toEtcdException(outcome.cause()));
            }
            return null;
        }, false)).onFailure(error -> LOG.warn("gRPC stream terminal callback failed", error));
    }

    private Future<Void> start() {
        response.handler(this::handleMessage);
        end.onComplete(outcome -> {
            if (!handlingMessage) {
                complete(outcome);
            }
        });
        if (!end.isComplete()) {
            response.fetch(1);
        }
        return result.future();
    }

    private void handleMessage(Resp message) {
        handlingMessage = true;
        Vertx.currentContext().executeBlocking(() -> {
            handler.handle(message);
            return null;
        }, false).onComplete(handled -> {
            if (handled.failed()) {
                // Cancel while demand is paused, then report the handler failure as the terminal error.
                Exceptions.quietly(response.request()::cancel);
                result.fail(handled.cause());
                return;
            }
            handlingMessage = false;
            if (end.isComplete()) {
                complete(end);
            } else {
                response.fetch(1);
            }
        });
    }

    private void complete(AsyncResult<Void> outcome) {
        if (outcome.succeeded()) {
            result.tryComplete();
        } else if (outcome.cause() instanceof InvalidStatusException failure) {
            result.tryFail(EtcdExceptionFactory.toEtcdException(failure.actualStatus(), response.statusMessage(), failure));
        } else {
            result.tryFail(outcome.cause());
        }
    }
}
