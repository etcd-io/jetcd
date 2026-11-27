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

package io.etcd.jetcd.common.vertx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedRunnable;
import dev.failsafe.spi.DefaultScheduledFuture;
import dev.failsafe.spi.Scheduler;

/**
 * Factory for creating Failsafe {@link Scheduler} instances that execute on the Vert.x event loop.
 *
 * <p>
 * This class provides integration between the <a href="https://failsafe.dev">Failsafe</a> resilience library
 * and Vert.x's asynchronous execution model. The schedulers created by this class leverage Vert.x's
 * lightweight timer mechanism ({@link Vertx#setTimer}) for delayed execution, eliminating the overhead
 * of traditional thread pools.
 * </p>
 *
 * <h2>Benefits</h2>
 * <ul>
 * <li><b>Zero thread overhead</b> - Uses Vert.x event loop instead of creating scheduler threads</li>
 * <li><b>Consistent execution model</b> - All retry operations execute on Vert.x event loop threads</li>
 * <li><b>Lightweight timers</b> - Vert.x timers are more efficient than {@code ScheduledExecutorService}</li>
 * <li><b>Non-blocking delays</b> - Retry delays don't block threads</li>
 * </ul>
 *
 * <h2>Usage with Failsafe</h2>
 *
 * <pre>
 * Vertx vertx = Vertx.vertx();
 * Scheduler scheduler = Schedulers.vertxScheduler(vertx);
 *
 * RetryPolicy&lt;Void&gt; retryPolicy = RetryPolicy.&lt;Void&gt; builder()
 *     .withMaxRetries(3)
 *     .withDelay(Duration.ofMillis(100))
 *     .build();
 *
 * CompletableFuture&lt;Void&gt; future = Failsafe.with(retryPolicy)
 *     .with(scheduler)
 *     .runAsync(() -> {
 *         // This executes on Vert.x event loop thread
 *         // Retries also happen on event loop with 100ms delays
 *         performAsyncOperation();
 *     });
 * </pre>
 *
 * <h2>Concurrency Considerations</h2>
 * <p>
 * Tasks scheduled with this scheduler will execute on Vert.x event loop threads. While this provides
 * excellent performance for non-blocking operations, be careful not to perform long-running or blocking
 * operations on the event loop as this will impact overall system throughput.
 * </p>
 *
 * @see <a href="https://failsafe.dev">Failsafe Documentation</a>
 * @see <a href="https://vertx.io/docs/vertx-core/java/#_timer">Vert.x Timer Documentation</a>
 */
public final class Failsafe {

    private Failsafe() {
        // Utility class, prevent instantiation
    }

    /**
     * Creates a Failsafe Scheduler that executes on the Vert.x event loop.
     *
     * <p>
     * The returned scheduler uses {@link Vertx#setTimer} for delayed execution and
     * {@link Vertx#runOnContext} for immediate execution (zero delay). This eliminates
     * the need for a separate thread pool while providing efficient retry scheduling.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Zero-delay executions use {@code vertx.getOrCreateContext().runOnContext()}</li>
     * <li>Delayed executions use {@code vertx.setTimer()}</li>
     * <li>Cancellation is supported via {@code vertx.cancelTimer()}</li>
     * <li>All tasks execute on Vert.x event loop threads</li>
     * </ul>
     *
     * <p>
     * Based on the official Failsafe VertxExample pattern for integrating with event-driven frameworks.
     * </p>
     *
     * @param  vertx the Vert.x instance to use for scheduling
     * @return       a Scheduler that executes tasks on the Vert.x event loop
     */
    public static Scheduler vertxScheduler(Vertx vertx) {
        return vertxScheduler(vertx, null);
    }

    /**
     * Creates a scheduler that dispatches attempts to a fixed Vert.x context.
     *
     * @param  context the context to use for each attempt
     * @return         a scheduler for the context
     */
    public static Scheduler vertxScheduler(Context context) {
        return vertxScheduler(context.owner(), context);
    }

    private static Scheduler vertxScheduler(Vertx vertx, Context context) {
        return (callable, delay, unit) -> {
            Runnable runnable = () -> {
                try {
                    callable.call();
                } catch (Exception ignore) {
                    // Failsafe handles exceptions internally
                }
            };

            final AtomicLong timerId = new AtomicLong();
            final long timerDelay = unit.toMillis(delay);

            return new DefaultScheduledFuture<>() {
                {
                    if (timerDelay == 0) {
                        Context target = context == null ? vertx.getOrCreateContext() : context;
                        target.runOnContext(v -> runnable.run());
                    } else {
                        timerId.set(
                            vertx.setTimer(timerDelay, tid -> {
                                if (context == null) {
                                    runnable.run();
                                } else {
                                    context.runOnContext(v -> runnable.run());
                                }
                            }));
                    }
                }

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return timerDelay != 0 && vertx.cancelTimer(timerId.get());
                }
            };
        };
    }

    /**
     * Execute an async task with retry policy on Vert.x event loop.
     * Integrates Failsafe retry logic with Vert.x scheduler for efficient async execution.
     *
     * @param  vertx       the Vert.x instance
     * @param  task        the task to execute
     * @param  retryPolicy the retry policy configuration
     * @return             a CompletableFuture representing the async execution
     */
    public static CompletableFuture<Void> runAsync(Vertx vertx, CheckedRunnable task, RetryPolicy<Void> retryPolicy) {
        return dev.failsafe.Failsafe.with(retryPolicy)
            .with(vertxScheduler(vertx))
            .runAsync(task);
    }

    /**
     * Execute a pipeline of async steps with retry policy on Vert.x event loop.
     * Each step is a supplier that returns a Vert.x Future. Steps are composed sequentially
     * and the entire pipeline is protected by the retry policy.
     *
     * <p>
     * Example usage:
     * </p>
     *
     * <pre>
     * Failsafe.pipeline(vertx, retryPolicy,
     *     this::disconnect,
     *     this::connect
     * ).whenComplete((r, e) -> { ... });
     * </pre>
     *
     * @param  vertx       the Vert.x instance
     * @param  retryPolicy the retry policy configuration
     * @param  steps       the pipeline steps to execute sequentially
     * @return             a CompletableFuture representing the pipeline execution
     */
    @SafeVarargs
    public static CompletableFuture<Void> pipeline(
        Vertx vertx,
        RetryPolicy<Void> retryPolicy,
        Supplier<Future<Void>>... steps) {

        return dev.failsafe.Failsafe.with(retryPolicy)
            .with(vertxScheduler(vertx))
            .getStageAsync(() -> {
                Future<Void> pipeline = Future.succeededFuture();
                for (Supplier<Future<Void>> step : steps) {
                    pipeline = pipeline.compose(v -> step.get());
                }
                return pipeline.toCompletionStage();
            });
    }
}
