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

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.common.vertx.Failsafe;
import io.etcd.jetcd.grpc.GrpcAuth.AuthenticatedRequest;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.support.Responses;
import io.vertx.core.Future;
import io.vertx.core.Timer;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpClosedException;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.WireFormat;

import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;

/**
 * Base class for service implementations providing common utilities.
 * Handles Future to CompletableFuture conversion and retry execution with Failsafe.
 */
abstract class AbstractClient {
    private final GrpcService grpcService;
    protected final Responses.Namespaced responseFactory;

    protected AbstractClient(GrpcService grpcService) {
        this.grpcService = grpcService;
        this.responseFactory = Responses.namespaced(grpcService.getNamespace());
    }

    protected GrpcService grpc() {
        return this.grpcService;
    }

    /**
     * Creates a retry policy for the operation.
     *
     * @param  retryTransportFailures whether this operation can be repeated after a transport failure
     * @return                        configured retry policy
     */
    protected <S> RetryPolicy<S> createRetryPolicy(boolean retryTransportFailures) {
        RetryPolicyBuilder<S> policy = RetryPolicy.<S> builder()
            .handleIf(error -> (error instanceof GrpcResponseException failure && failure.tokenRejected)
                || (retryTransportFailures && isTransportFailure(originalFailure(error))))
            .withMaxRetries(grpcService.builder().retryMaxAttempts())
            .withBackoff(
                grpcService.builder().retryDelay(),
                grpcService.builder().retryMaxDelay(),
                grpcService.builder().retryChronoUnit());

        if (grpcService.builder().retryMaxDuration() != null) {
            policy = policy.withMaxDuration(grpcService.builder().retryMaxDuration());
        }

        return policy.build();
    }

    private static boolean isTransportFailure(Throwable error) {
        if (error instanceof InvalidStatusException status) {
            return status.actualStatus() == GrpcStatus.UNAVAILABLE;
        }
        if (error instanceof HttpClosedException || error instanceof ConnectException
            || error instanceof ClosedChannelException) {
            return true;
        }
        // Vert.x can report a socket closed during HTTP/2 setup as a plain VertxException.
        return error instanceof VertxException && "Connection was closed".equals(error.getMessage());
    }

    /**
     * Converts Future of Type S to CompletableFuture of Type T.
     *
     * @param  sourceFuture  the Future to wrap
     * @param  resultConvert the result converter
     * @return               a {@link CompletableFuture} wrapping the given {@link Future}
     */
    protected <S, T> CompletableFuture<T> completable(Future<S> sourceFuture, Function<S, T> resultConvert) {
        return completable(sourceFuture, resultConvert, EtcdExceptionFactory::toEtcdException);
    }

    /**
     * Converts Future of Type S to CompletableFuture of Type T.
     *
     * @param  sourceFuture       the Future to wrap
     * @param  resultConvert      the result converter
     * @param  exceptionConverter the exception mapper
     * @return                    a {@link CompletableFuture} wrapping the given {@link Future}
     */
    protected <S, T> CompletableFuture<T> completable(
        Future<S> sourceFuture,
        Function<S, T> resultConvert,
        Function<Throwable, Throwable> exceptionConverter) {

        return completable(
            sourceFuture.compose(
                r -> Future.succeededFuture(resultConvert.apply(r)),
                e -> Future.failedFuture(exceptionConverter.apply(e))));
    }

    /**
     * Converts Future of Type S to CompletableFuture of Type T.
     *
     * @param  sourceFuture the Future to wrap
     * @return              a {@link CompletableFuture} wrapping the given {@link Future}
     */
    protected <S> CompletableFuture<S> completable(
        Future<S> sourceFuture) {
        return sourceFuture.toCompletionStage().toCompletableFuture();
    }

    protected <Req, Resp, T> CompletableFuture<T> execute(
        ServiceMethod<Resp, Req> method,
        Req message,
        Function<Resp, T> resultConvert,
        boolean retryTransportFailures) {

        return execute(method, message, resultConvert, retryTransportFailures, AbstractClient::originalFailure);
    }

    protected <Req, Resp, T> CompletableFuture<T> execute(
        ServiceMethod<Resp, Req> method,
        Req message,
        Function<Resp, T> resultConvert,
        boolean retryTransportFailures,
        Function<Throwable, Throwable> exceptionConvert) {

        return execute(() -> grpcService.auth().request(method).compose(rpc -> send(rpc, message)), resultConvert,
            retryTransportFailures, exceptionConvert);
    }

    protected <S, T> CompletableFuture<T> execute(
        Supplier<Future<S>> supplier,
        Function<S, T> resultConvert,
        boolean retryTransportFailures) {

        return execute(supplier, resultConvert, retryTransportFailures, AbstractClient::originalFailure);
    }

    private <S, T> CompletableFuture<T> execute(
        Supplier<Future<S>> supplier,
        Function<S, T> resultConvert,
        boolean retryTransportFailures,
        Function<Throwable, Throwable> exceptionConvert) {

        return this.<S> retryExecutor(retryTransportFailures)
            .getStageAsync(() -> supplier.get().toCompletionStage())
            .thenApply(resultConvert)
            .exceptionallyCompose(error -> CompletableFuture.failedFuture(exceptionConvert.apply(unwrapCompletion(error))));
    }

    protected <Req, Resp, T> CompletableFuture<T> executeCancellable(
        Supplier<Future<AuthenticatedRequest<Req, Resp>>> requestSupplier,
        Req request,
        Function<Resp, T> resultConvert,
        boolean retryTransportFailures,
        Function<Throwable, Throwable> exceptionConvert) {

        return executeCancellable(requestSupplier, request, resultConvert, retryTransportFailures, exceptionConvert, null);
    }

    protected <Req, Resp, T> CompletableFuture<T> executeWithDeadline(
        ServiceMethod<Resp, Req> method,
        Req request,
        Function<Resp, T> resultConvert,
        long timeout,
        TimeUnit unit) {

        if (timeout <= 0) {
            return CompletableFuture.failedFuture(deadlineExceeded());
        }
        final Timer deadline;
        try {
            deadline = grpcService.vertx().timer(timeout, unit);
        } catch (RejectedExecutionException error) {
            return CompletableFuture.failedFuture(error);
        }
        return executeCancellable(() -> grpcService.auth().request(method), request, resultConvert, false,
            AbstractClient::convertDeadlineFailure, deadline);
    }

    private <Req, Resp, T> CompletableFuture<T> executeCancellable(
        Supplier<Future<AuthenticatedRequest<Req, Resp>>> requestSupplier,
        Req request,
        Function<Resp, T> resultConvert,
        boolean retryTransportFailures,
        Function<Throwable, Throwable> exceptionConvert,
        Timer deadline) {

        var result = new CompletableFuture<T>();
        var execution = this.<Resp> retryExecutor(retryTransportFailures).getStageAsync(attempt -> {
            // A retry racing with cancellation may receive a new Failsafe attempt context.
            if (result.isDone() || attempt.isCancelled()) {
                return CompletableFuture.failedFuture(new CancellationException());
            }
            if (hasExpired(deadline)) {
                return CompletableFuture.failedFuture(deadlineExceeded());
            }
            return requestSupplier.get().compose(authenticated -> {
                var rpc = authenticated.request();
                var context = grpcService.vertx().getOrCreateContext();
                attempt.onCancel(() -> context.runOnContext(ignored -> rpc.cancel()));
                // The attempt may have been cancelled before the request became available.
                if (result.isDone() || attempt.isCancelled()) {
                    rpc.cancel();
                    return Future.failedFuture(new CancellationException());
                }
                if (deadline != null) {
                    long remaining = deadline.getDelay(TimeUnit.NANOSECONDS);
                    if (remaining <= 0) {
                        rpc.cancel();
                        return Future.failedFuture(deadlineExceeded());
                    }
                    rpc.timeout(remaining, TimeUnit.NANOSECONDS);
                }
                return send(authenticated, request);
            }).toCompletionStage();
        });

        var unusedCancellation = result.whenComplete((value, error) -> {
            if (error != null) {
                execution.cancel(false);
            }
            if (deadline != null) {
                deadline.cancel();
            }
        });
        if (deadline != null) {
            deadline.onSuccess(ignored -> result.completeExceptionally(deadlineExceeded()))
                .onFailure(result::completeExceptionally);
        }
        var unusedCompletion = execution.thenApply(resultConvert)
            .exceptionallyCompose(error -> CompletableFuture.failedFuture(exceptionConvert.apply(unwrapCompletion(error))))
            .whenComplete((value, error) -> {
                if (hasExpired(deadline)) {
                    result.completeExceptionally(deadlineExceeded());
                } else if (error == null) {
                    result.complete(value);
                } else {
                    result.completeExceptionally(error);
                }
            });
        return result;
    }

    private static boolean hasExpired(Timer deadline) {
        return deadline != null && deadline.getDelay(TimeUnit.NANOSECONDS) <= 0;
    }

    private static EtcdException deadlineExceeded() {
        return EtcdExceptionFactory.newEtcdException(ErrorCode.DEADLINE_EXCEEDED, "Request deadline exceeded");
    }

    private static Throwable convertDeadlineFailure(Throwable error) {
        Throwable cause = originalFailure(error);
        if (cause instanceof InvalidStatusException status && status.actualStatus() == GrpcStatus.DEADLINE_EXCEEDED) {
            return EtcdExceptionFactory.newEtcdException(ErrorCode.DEADLINE_EXCEEDED, "Request deadline exceeded", status);
        }
        return cause;
    }

    private <S> FailsafeExecutor<S> retryExecutor(boolean retryTransportFailures) {
        return dev.failsafe.Failsafe.with(this.<S> createRetryPolicy(retryTransportFailures))
            .with(Failsafe.vertxScheduler(grpcService.vertx()));
    }

    private static <Req, Resp> Future<Resp> send(AuthenticatedRequest<Req, Resp> authenticated, Req message) {
        var rpc = authenticated.request();
        return rpc.format(WireFormat.PROTOBUF).end(message)
            .compose(ignored -> rpc.response())
            .compose(response -> response.last().recover(error -> {
                // Vert.x exceptions carry the status code. The response carries the server's message.
                if (error instanceof InvalidStatusException status) {
                    String messageFromServer = response.statusMessage();
                    boolean tokenRejected = Errors.isRefreshableAuthError(status.actualStatus(), messageFromServer)
                        && authenticated.invalidateToken();
                    return Future.failedFuture(new GrpcResponseException(status, messageFromServer, tokenRejected));
                }
                return Future.failedFuture(error);
            }));
    }

    protected static Throwable originalFailure(Throwable error) {
        return error instanceof GrpcResponseException ? error.getCause() : error;
    }

    private static Throwable unwrapCompletion(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    /** Retains the response details while the retry policy evaluates the failure. */
    protected static final class GrpcResponseException extends RuntimeException {
        /** The status reported by the server. */
        private final GrpcStatus status;
        /** Whether the server rejected a managed token before executing the request. */
        private final boolean tokenRejected;

        private GrpcResponseException(InvalidStatusException cause, String message, boolean tokenRejected) {
            super(message, cause, false, false);
            this.status = cause.actualStatus();
            this.tokenRejected = tokenRejected;
        }

        GrpcStatus status() {
            return status;
        }
    }
}
