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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.RetryContext;
import io.etcd.jetcd.watch.WatchResponse;
import io.etcd.jetcd.watch.WatchState;
import io.vertx.core.Vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("watch")
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class WatchReconnectionManagerTest {

    private Vertx vertx;
    private WatchStateMachine stateMachine;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        stateMachine = createStateMachine();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testSuccessfulReconnectionOnFirstAttempt() throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Watch.Listener listener = createListener(ctx -> retryCount.incrementAndGet());
        WatchOption option = WatchOption.builder().build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        manager.attemptReconnection(
            () -> {
                // Successful disconnect - no exception
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    succeeded.set(true);
                    latch.countDown();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    latch.countDown();
                }
            });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(succeeded.get()).isTrue();
        assertThat(retryCount.get()).isEqualTo(0); // No retries on first success
    }

    @Test
    public void testReconnectionAfterRetries() throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicInteger disconnectAttempts = new AtomicInteger(0);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Watch.Listener listener = createListener(ctx -> retryCount.incrementAndGet());
        WatchOption option = WatchOption.builder()
            .withMaxReconnectAttempts(5)
            .withInitialReconnectDelay(Duration.ofMillis(50))
            .withMaxReconnectDelay(Duration.ofMillis(200))
            .withReconnectJitter(Duration.ofMillis(25))
            .build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        manager.attemptReconnection(
            () -> {
                int attempt = disconnectAttempts.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException("Simulated failure " + attempt);
                }
                // Third attempt succeeds
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    succeeded.set(true);
                    latch.countDown();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    latch.countDown();
                }
            });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(succeeded.get()).isTrue();
        assertThat(retryCount.get()).isEqualTo(2); // 2 failures before success
        assertThat(disconnectAttempts.get()).isEqualTo(3); // Total attempts
    }

    @Test
    public void testReconnectionFailureAfterMaxAttempts() throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicReference<Throwable> failureError = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Watch.Listener listener = createListener(ctx -> retryCount.incrementAndGet());
        WatchOption option = WatchOption.builder()
            .withMaxReconnectAttempts(3)
            .withInitialReconnectDelay(Duration.ofMillis(50))
            .withMaxReconnectDelay(Duration.ofMillis(100))
            .withReconnectJitter(Duration.ofMillis(25))
            .build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        manager.attemptReconnection(
            () -> {
                throw new RuntimeException("Always fails");
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    latch.countDown();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    failureError.set(error);
                    latch.countDown();
                }
            });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(failureError.get()).isNotNull();
        assertThat(retryCount.get()).isEqualTo(3); // 3 retries = 3 failure notifications
    }

    @Test
    public void testConcurrentReconnectionAttempts() throws Exception {
        AtomicInteger disconnectCount = new AtomicInteger(0);
        AtomicInteger callbackCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Watch.Listener listener = createListener(ctx -> {
        });
        WatchOption option = WatchOption.builder()
            .withInitialReconnectDelay(Duration.ofMillis(100))
            .withReconnectJitter(Duration.ofMillis(50))
            .build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        // Start first reconnection
        manager.attemptReconnection(
            () -> {
                disconnectCount.incrementAndGet();
                try {
                    Thread.sleep(200); // Simulate slow disconnect
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    callbackCount.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    callbackCount.incrementAndGet();
                    latch.countDown();
                }
            });

        // Immediately try second reconnection - should be ignored
        manager.attemptReconnection(
            () -> disconnectCount.incrementAndGet(),
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    callbackCount.incrementAndGet();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    callbackCount.incrementAndGet();
                }
            });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(disconnectCount.get()).isEqualTo(1); // Only one disconnect executed
        assertThat(callbackCount.get()).isEqualTo(1); // Only first callback invoked
    }

    @Test
    public void testCancelReconnection() throws Exception {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        Watch.Listener listener = createListener(ctx -> {
        });
        WatchOption option = WatchOption.builder()
            .withMaxReconnectAttempts(10)
            .withInitialReconnectDelay(Duration.ofMillis(100))
            .withReconnectJitter(Duration.ofMillis(50))
            .build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        // Start reconnection that will fail and retry
        manager.attemptReconnection(
            () -> {
                throw new RuntimeException("Failure");
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    callbackInvoked.set(true);
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    callbackInvoked.set(true);
                }
            });

        // Verify reconnection is in progress
        await().atMost(1, TimeUnit.SECONDS).until(manager::isReconnecting);

        // Cancel it
        manager.cancelReconnection();

        // Verify it's no longer reconnecting
        assertThat(manager.isReconnecting()).isFalse();

        // Wait a bit to ensure callback is not invoked after cancellation
        Thread.sleep(500);
        assertThat(callbackInvoked.get()).isFalse();
    }

    @Test
    public void testCancelWhenNotReconnecting() {
        Watch.Listener listener = createListener(ctx -> {
        });
        WatchOption option = WatchOption.builder().build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        // Should be safe to call when no reconnection is active
        manager.cancelReconnection();
        assertThat(manager.isReconnecting()).isFalse();
    }

    @Test
    public void testReconnectionStoppedWhenWatchClosed() throws Exception {
        AtomicInteger retryNotificationCount = new AtomicInteger(0);
        AtomicInteger attemptCount = new AtomicInteger(0);

        Watch.Listener listener = createListener(ctx -> retryNotificationCount.incrementAndGet());
        WatchOption option = WatchOption.builder()
            .withMaxReconnectAttempts(10)
            .withInitialReconnectDelay(Duration.ofMillis(50))
            .withReconnectJitter(Duration.ofMillis(25))
            .build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        manager.attemptReconnection(
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt == 3) {
                    // Close the watch after third attempt
                    stateMachine.close();
                }
                throw new RuntimeException("Failure " + attempt);
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                }
            });

        // Wait for watch to be closed and some retry attempts
        await().atMost(2, TimeUnit.SECONDS).until(() -> stateMachine.isClosed());

        // After closing, no more retry notifications should be sent
        // The retry callback checks if watch is closed and skips notification
        int notificationsBeforeClose = retryNotificationCount.get();
        Thread.sleep(300); // Give time for any pending notifications
        assertThat(retryNotificationCount.get()).isEqualTo(notificationsBeforeClose);
    }

    @Test
    public void testCustomReconnectionParameters() throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Watch.Listener listener = createListener(ctx -> {
            retryCount.incrementAndGet();
            assertThat(ctx.maxAttempts()).isEqualTo(3);
        });

        WatchOption option = WatchOption.builder()
            .withMaxReconnectAttempts(3)
            .withInitialReconnectDelay(Duration.ofMillis(100))
            .withMaxReconnectDelay(Duration.ofSeconds(5))
            .withReconnectJitter(Duration.ofMillis(50))
            .build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        manager.attemptReconnection(
            () -> {
                throw new RuntimeException("Always fails");
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    latch.countDown();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    latch.countDown();
                }
            });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(retryCount.get()).isEqualTo(3); // Exactly 3 retries as configured
    }

    private WatchStateMachine createStateMachine() {
        return new WatchStateMachine(new WatchStateMachine.Handler() {
            @Override
            public void onConnect() {
            }

            @Override
            public void onSubscribe() {
            }

            @Override
            public void onReady() {
            }

            @Override
            public void onReconnect() {
            }

            @Override
            public void onClose() {
            }

            @Override
            public void onError(Throwable error) {
            }

            @Override
            public void onStateChange(WatchStateMachine.State oldState, WatchStateMachine.State newState) {
            }
        });
    }

    @Test
    public void testReconnectionWithJitter() throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicInteger disconnectAttempts = new AtomicInteger(0);
        List<Long> retryTimestamps = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Watch.Listener listener = createListener(ctx -> {
            retryTimestamps.add(System.currentTimeMillis());
            retryCount.incrementAndGet();
        });

        WatchOption option = WatchOption.builder()
            .withMaxReconnectAttempts(5)
            .withInitialReconnectDelay(Duration.ofMillis(100))
            .withMaxReconnectDelay(Duration.ofMillis(200))
            .withReconnectJitter(Duration.ofMillis(50))
            .build();

        WatchReconnectionManager manager = new WatchReconnectionManager(vertx.getOrCreateContext(), option, listener::onRetry,
            stateMachine);

        manager.attemptReconnection(
            () -> {
                int attempt = disconnectAttempts.incrementAndGet();
                if (attempt < 5) {
                    throw new RuntimeException("Simulated failure " + attempt);
                }
            },
            new WatchReconnectionManager.ReconnectionCallback() {
                @Override
                public void onReconnectSucceeded() {
                    latch.countDown();
                }

                @Override
                public void onReconnectFailed(Throwable error) {
                    latch.countDown();
                }
            });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(retryTimestamps).hasSizeGreaterThanOrEqualTo(3);

        // Verify jitter is applied - delays should vary
        List<Long> delays = new ArrayList<>();
        for (int i = 1; i < retryTimestamps.size(); i++) {
            delays.add(retryTimestamps.get(i) - retryTimestamps.get(i - 1));
        }

        // All delays should be within expected range (100ms base + 50ms jitter = 100-150ms range)
        for (Long delay : delays) {
            assertThat(delay).isBetween(95L, 250L); // Allow some timing variance
        }

        // Delays should not all be identical (jitter adds variance)
        Set<Long> uniqueDelays = new HashSet<>(delays);
        if (delays.size() >= 3) {
            assertThat(uniqueDelays.size()).isGreaterThan(1);
        }
    }

    @Test
    public void testCustomJitterConfiguration() {
        WatchOption optionWithJitter = WatchOption.builder()
            .withReconnectJitter(Duration.ofMillis(250))
            .build();

        assertThat(optionWithJitter.reconnectJitter()).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    public void testMinimalJitterConfiguration() {
        Watch.Listener listener = createListener(ctx -> {
        });
        WatchOption optionMinimalJitter = WatchOption.builder()
            .withReconnectJitter(Duration.ofMillis(1))
            .build();

        assertThat(optionMinimalJitter.reconnectJitter()).isEqualTo(Duration.ofMillis(1));

        // Minimal jitter should still work
        WatchReconnectionManager manager = new WatchReconnectionManager(
            vertx.getOrCreateContext(), optionMinimalJitter, listener::onRetry, stateMachine);

        assertThat(manager).isNotNull();
    }

    private Watch.Listener createListener(java.util.function.Consumer<RetryContext> onRetry) {
        return new Watch.Listener() {
            @Override
            public void onNext(WatchResponse response) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onRetry(RetryContext context) {
                onRetry.accept(context);
            }

            @Override
            public void onStateChange(WatchState oldState, WatchState newState) {
            }
        };
    }
}
