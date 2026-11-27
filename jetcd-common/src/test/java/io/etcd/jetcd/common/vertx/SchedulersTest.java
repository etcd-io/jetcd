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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import dev.failsafe.RetryPolicy;
import dev.failsafe.spi.Scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulersTest {
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx(new VertxOptions().setUseDaemonThread(true));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testVertxSchedulerExecutesOnEventLoop() throws Exception {
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Scheduler scheduler = Failsafe.vertxScheduler(vertx);
        RetryPolicy<Void> policy = RetryPolicy.<Void> builder().build();

        @SuppressWarnings("unused")
        var unused = dev.failsafe.Failsafe.with(policy)
            .with(scheduler)
            .runAsync(() -> {
                threadName.set(Thread.currentThread().getName());
                latch.countDown();
            });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(threadName.get().contains("vert.x-eventloop"),
            "Expected vert.x event loop thread but got: " + threadName.get());
    }

    @Test
    void testVertxSchedulerRetryWithBackoff() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicReference<Long> firstAttemptTime = new AtomicReference<>();
        AtomicReference<Long> secondAttemptTime = new AtomicReference<>();

        Scheduler scheduler = Failsafe.vertxScheduler(vertx);
        RetryPolicy<Void> policy = RetryPolicy.<Void> builder()
            .withMaxRetries(3)
            .withDelay(Duration.ofMillis(100))
            .build();

        CompletableFuture<Void> future = new CompletableFuture<>();

        @SuppressWarnings("unused")
        var unused = dev.failsafe.Failsafe.with(policy)
            .with(scheduler)
            .getAsyncExecution(execution -> {
                int attempt = attempts.incrementAndGet();
                if (attempt == 1) {
                    firstAttemptTime.set(System.currentTimeMillis());
                } else if (attempt == 2) {
                    secondAttemptTime.set(System.currentTimeMillis());
                }

                if (attempt < 3) {
                    execution.recordException(new RuntimeException("Retry " + attempt));
                } else {
                    execution.recordResult(null);
                    future.complete(null);
                }
            });

        future.get(2, TimeUnit.SECONDS);
        assertEquals(3, attempts.get());

        long delay = secondAttemptTime.get() - firstAttemptTime.get();
        assertTrue(delay >= 95 && delay <= 200,
            "Expected ~100ms delay between retries, got " + delay + "ms");
    }

    @Test
    void testVertxSchedulerImmediateExecution() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();

        Scheduler scheduler = Failsafe.vertxScheduler(vertx);
        RetryPolicy<Void> policy = RetryPolicy.<Void> builder().build();

        @SuppressWarnings("unused")
        var unused = dev.failsafe.Failsafe.with(policy)
            .with(scheduler)
            .runAsync(() -> latch.countDown());

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 50, "Immediate execution took " + elapsed + "ms");
    }

    @Test
    void testVertxSchedulerConcurrentRetries() throws Exception {
        int concurrentTasks = 5;
        CountDownLatch latch = new CountDownLatch(concurrentTasks);
        AtomicInteger totalAttempts = new AtomicInteger(0);

        Scheduler scheduler = Failsafe.vertxScheduler(vertx);
        RetryPolicy<Void> policy = RetryPolicy.<Void> builder()
            .withMaxRetries(2)
            .withDelay(Duration.ofMillis(50))
            .build();

        for (int i = 0; i < concurrentTasks; i++) {
            int taskId = i;
            AtomicInteger taskAttempts = new AtomicInteger(0);

            @SuppressWarnings("unused")
            var unused = dev.failsafe.Failsafe.with(policy)
                .with(scheduler)
                .getAsyncExecution(execution -> {
                    int attempt = taskAttempts.incrementAndGet();
                    totalAttempts.incrementAndGet();

                    if (attempt < 2) {
                        execution.recordException(new RuntimeException("Task " + taskId + " retry " + attempt));
                    } else {
                        execution.recordResult(null);
                        latch.countDown();
                    }
                });
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS), "All concurrent tasks should complete");
        assertEquals(concurrentTasks * 2, totalAttempts.get(),
            "Each task should attempt exactly 2 times");
    }

    @Test
    void testVertxSchedulerCancellation() throws Exception {
        Scheduler scheduler = Failsafe.vertxScheduler(vertx);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger(0);

        var future = scheduler.schedule(() -> {
            executions.incrementAndGet();
            latch.countDown();
            return null;
        }, 5000, TimeUnit.MILLISECONDS);

        // Cancel before it executes
        boolean cancelled = future.cancel(false);
        assertTrue(cancelled, "Should be able to cancel delayed execution");

        // Wait to ensure it doesn't execute
        assertFalse(latch.await(100, TimeUnit.MILLISECONDS));
        assertEquals(0, executions.get(), "Cancelled task should not execute");
    }
}
