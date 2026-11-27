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
import java.util.function.Consumer;

import io.etcd.jetcd.common.exception.Exceptions;
import io.etcd.jetcd.common.vertx.Failsafe;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.RetryContext;
import io.vertx.core.Context;

import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedRunnable;

/**
 * Manages reconnection attempts for watch connections using exponential backoff.
 * Encapsulates Failsafe retry logic and listener notifications.
 *
 * <p>
 * This class is responsible for:
 * </p>
 * <ul>
 * <li>Building retry policies based on WatchOption configuration</li>
 * <li>Managing reconnection attempts with exponential backoff</li>
 * <li>Tracking in-progress reconnection futures</li>
 * <li>Notifying listeners of retry attempts</li>
 * <li>Handling reconnection cancellation</li>
 * </ul>
 *
 * <p>
 * Thread Safety: All operations should be called from the Vert.x event loop thread.
 * </p>
 */
final class WatchReconnectionManager {

    /**
     * Callback interface for reconnection results.
     */
    interface ReconnectionCallback {
        /**
         * Called when reconnection succeeds after retry.
         */
        void onReconnectSucceeded();

        /**
         * Called when reconnection fails after exhausting all retries.
         *
         * @param error the failure cause
         */
        void onReconnectFailed(Throwable error);
    }

    private final Context context;
    private final WatchOption option;
    private final Consumer<RetryContext> onRetry;
    private final WatchStateMachine stateMachine;
    private final RetryPolicy<Void> retryPolicy;

    private volatile CompletableFuture<Void> reconnectFuture;

    /**
     * Creates a new reconnection manager.
     *
     * @param context      the owning Vert.x context
     * @param option       watch configuration with reconnection parameters
     * @param onRetry      notification of retry attempts
     * @param stateMachine state machine to check if watch is closed
     */
    WatchReconnectionManager(
        Context context,
        WatchOption option,
        Consumer<RetryContext> onRetry,
        WatchStateMachine stateMachine) {

        this.context = context;
        this.option = option;
        this.onRetry = onRetry;
        this.stateMachine = stateMachine;
        this.retryPolicy = buildRetryPolicy();
    }

    /**
     * Attempts reconnection with retry logic.
     * If a reconnection is already in progress, this is a no-op.
     *
     * @param  disconnectAction     action to run before reconnecting (e.g., cleanup)
     * @param  callback             callback for reconnection result
     * @throws NullPointerException if callback is null
     */
    void attemptReconnection(CheckedRunnable disconnectAction, ReconnectionCallback callback) {
        java.util.Objects.requireNonNull(callback, "callback cannot be null");

        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            return;
        }

        var attempt = dev.failsafe.Failsafe.with(retryPolicy)
            .with(Failsafe.vertxScheduler(context))
            .runAsync(disconnectAction);
        reconnectFuture = attempt;
        // The original future owns cancellation and identifies which attempt completed.
        @SuppressWarnings("ReferenceEquality")
        var unused = attempt.whenComplete((result, error) -> {
            if (reconnectFuture != attempt) {
                return;
            }
            reconnectFuture = null;
            if (error == null) {
                callback.onReconnectSucceeded();
            } else {
                callback.onReconnectFailed(error);
            }
        });
    }

    /**
     * Cancels any in-progress reconnection attempt.
     * Safe to call multiple times or when no reconnection is active.
     */
    void cancelReconnection() {
        var attempt = reconnectFuture;
        reconnectFuture = null;
        if (attempt != null) {
            attempt.cancel(false);
        }
    }

    /**
     * Returns whether a reconnection attempt is currently in progress.
     *
     * @return true if reconnecting
     */
    boolean isReconnecting() {
        var attempt = reconnectFuture;
        return attempt != null && !attempt.isDone();
    }

    private RetryPolicy<Void> buildRetryPolicy() {
        return RetryPolicy.<Void> builder()
            .withMaxRetries(option.maxReconnectAttempts())
            .withBackoff(option.initialReconnectDelay(), option.maxReconnectDelay())
            .withJitter(option.reconnectJitter())
            .onRetry(e -> {
                if (stateMachine.isClosed()) {
                    return;
                }

                RetryContext ctx = RetryContext.of(RetryContext.RetryType.RESUME)
                    .attemptCount(e.getAttemptCount())
                    .maxAttempts(option.maxReconnectAttempts())
                    .cause(e.getLastException())
                    .build();

                Exceptions.quietly(() -> onRetry.accept(ctx));
            })
            .build();
    }
}
