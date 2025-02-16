package io.etcd.jetcd.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.support.Errors;
import io.grpc.Status;
import io.vertx.core.Future;

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;

import static io.etcd.jetcd.support.Errors.isAuthStoreExpired;
import static io.etcd.jetcd.support.Errors.isInvalidTokenError;

abstract class Impl {
    private final Logger logger;
    private final ClientConnectionManager connectionManager;

    protected Impl(ClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    protected ClientConnectionManager connectionManager() {
        return this.connectionManager;
    }

    protected Logger logger() {
        return this.logger;
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

    /**
     * execute the task and retry it in case of failure.
     *
     * @param  supplier      a function that returns a new Future.
     * @param  resultConvert a function that converts Type S to Type T.
     * @param  <S>           Source type
     * @param  <T>           Converted Type.
     * @return               a CompletableFuture with type T.
     */
    protected <S, T> CompletableFuture<T> execute(
        Supplier<Future<S>> supplier,
        Function<S, T> resultConvert,
        boolean autoRetry) {

        return execute(supplier, resultConvert,
            autoRetry ? Errors::isRetryableForSafeRedoOp : Errors::isRetryableForNoSafeRedoOp);
    }

    /**
     * execute the task and retry it in case of failure.
     *
     * @param  supplier      a function that returns a new Future.
     * @param  resultConvert a function that converts Type S to Type T.
     * @param  doRetry       a predicate to determine if a failure has to be retried
     * @param  <S>           Source type
     * @param  <T>           Converted Type.
     * @return               a CompletableFuture with type T.
     */
    protected <S, T> CompletableFuture<T> execute(
        Supplier<Future<S>> supplier,
        Function<S, T> resultConvert,
        Predicate<Status> doRetry) {

        return Failsafe
            .with(retryPolicy(doRetry))
            .with(connectionManager.getExecutorService())
            .getStageAsync(() -> supplier.get().toCompletionStage())
            .thenApply(resultConvert);
    }

    protected <S> RetryPolicy<S> retryPolicy(Predicate<Status> doRetry) {
        RetryPolicyBuilder<S> policy = RetryPolicy.<S> builder()
            .onFailure(e -> {
                logger.warn("retry failure (attempt: {}, error: {})",
                    e.getAttemptCount(),
                    e.getException() != null ? e.getException().getMessage() : "<none>");
            })
            .onRetry(e -> {
                logger.debug("retry (attempt: {}, error: {})",
                    e.getAttemptCount(),
                    e.getLastException() != null ? e.getLastException().getMessage() : "<none>");
            })
            .onRetriesExceeded(e -> {
                logger.warn("maximum number of auto retries reached (attempt: {}, error: {})",
                    e.getAttemptCount(),
                    e.getException() != null ? e.getException().getMessage() : "<none>");
            })
            .handleIf(throwable -> {
                Status status = Status.fromThrowable(throwable);
                if (isInvalidTokenError(status)) {
                    connectionManager.authCredential().refresh();
                }
                if (isAuthStoreExpired(status)) {
                    connectionManager.authCredential().refresh();
                }
                return doRetry.test(status);
            })
            .withMaxRetries(connectionManager.builder().retryMaxAttempts())
            .withBackoff(
                connectionManager.builder().retryDelay(),
                connectionManager.builder().retryMaxDelay(),
                connectionManager.builder().retryChronoUnit());

        if (connectionManager.builder().retryMaxDuration() != null) {
            policy = policy.withMaxDuration(connectionManager.builder().retryMaxDuration());
        }

        return policy.build();
    }
}
