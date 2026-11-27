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

package io.etcd.jetcd;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.watch.RetryContext;
import io.etcd.jetcd.watch.WatchResponse;
import io.etcd.jetcd.watch.WatchState;

/**
 * Interface of the watch client.
 */
public interface Watch extends CloseableClient {

    /**
     * watch on a key with option.
     *
     * @param  key                   key to be watched on.
     * @param  option                see {@link io.etcd.jetcd.options.WatchOption}.
     * @param  listener              the event consumer
     * @return                       this watcher
     * @throws ClosedClientException if watch client has been closed.
     */
    Watcher watch(ByteSequence key, WatchOption option, Listener listener);

    /**
     * Watch on a key with option, returning a future that completes when the watch is ready.
     *
     * <p>
     * The returned future completes when the etcd server confirms the watch is created.
     * This is useful when you need to ensure the watch is active before performing operations
     * that should be observed.
     * </p>
     *
     * @param  key                   key to be watched on.
     * @param  option                see {@link io.etcd.jetcd.options.WatchOption}.
     * @param  listener              the event consumer
     * @return                       future that completes with the watcher when watch is ready
     * @throws ClosedClientException if watch client has been closed.
     */
    CompletableFuture<Watcher> watchAsync(ByteSequence key, WatchOption option, Listener listener);

    /**
     * Watch on a key, returning a future that completes when the watch is ready.
     *
     * @param  key                   key to be watched on.
     * @param  listener              the event consumer
     * @return                       future that completes with the watcher when watch is ready
     * @throws ClosedClientException if watch client has been closed.
     */
    default CompletableFuture<Watcher> watchAsync(ByteSequence key, Listener listener) {
        return watchAsync(key, WatchOption.DEFAULT, listener);
    }

    /**
     * Watch on a key, returning a future that completes when the watch is ready.
     *
     * @param  key    key to be watched on.
     * @param  onNext the on next consumer
     * @return        future that completes with the watcher when watch is ready
     */
    default CompletableFuture<Watcher> watchAsync(ByteSequence key, Consumer<WatchResponse> onNext) {
        return watchAsync(key, WatchOption.DEFAULT, listener(onNext));
    }

    /**
     * watch on a key.
     *
     * @param  key                   key to be watched on.
     * @param  listener              the event consumer
     * @return                       this watcher
     * @throws ClosedClientException if watch client has been closed.
     **/
    default Watcher watch(ByteSequence key, Listener listener) {
        return watch(key, WatchOption.DEFAULT, listener);
    }

    /**
     * Watch key.
     *
     * @param  key    key to be watched on.
     * @param  onNext the on next consumer
     * @return        this watcher
     */
    default Watcher watch(ByteSequence key, Consumer<WatchResponse> onNext) {
        return watch(key, WatchOption.DEFAULT, listener(onNext));
    }

    /**
     * Watch key.
     *
     * @param  key     key to be watched on.
     * @param  onNext  the on next consumer
     * @param  onError the on error consumer
     * @return         this watcher
     */
    default Watcher watch(ByteSequence key, Consumer<WatchResponse> onNext, Consumer<Throwable> onError) {
        return watch(key, WatchOption.DEFAULT, listener(onNext, onError));
    }

    /**
     * Watch key.
     *
     * @param  key         key to be watched on.
     * @param  onNext      the on next consumer
     * @param  onError     the on error consumer
     * @param  onCompleted the on completion consumer
     * @return             this watcher
     */
    default Watcher watch(ByteSequence key, Consumer<WatchResponse> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        return watch(key, WatchOption.DEFAULT, listener(onNext, onError, onCompleted));
    }

    /**
     * Watch key.
     *
     * @param  key         key to be watched on.
     * @param  onNext      the on next consumer
     * @param  onCompleted the on completion consumer
     * @return             this watcher
     */
    default Watcher watch(ByteSequence key, Consumer<WatchResponse> onNext, Runnable onCompleted) {
        return watch(key, WatchOption.DEFAULT, listener(onNext, t -> {
        }, onCompleted));
    }

    /**
     * Watch key with option.
     *
     * @param  key    key to be watched on.
     * @param  option the options
     * @param  onNext the on next consumer
     * @return        this watcher
     */
    default Watcher watch(ByteSequence key, WatchOption option, Consumer<WatchResponse> onNext) {
        return watch(key, option, listener(onNext));
    }

    /**
     * Watch key with option.
     *
     * @param  key     key to be watched on.
     * @param  option  the options
     * @param  onNext  the on next consumer
     * @param  onError the on error consumer
     * @return         this watcher
     */
    default Watcher watch(ByteSequence key, WatchOption option, Consumer<WatchResponse> onNext, Consumer<Throwable> onError) {
        return watch(key, option, listener(onNext, onError));
    }

    /**
     * Watch key with option.
     *
     * @param  key         key to be watched on.
     * @param  option      the options
     * @param  onNext      the on next consumer
     * @param  onCompleted the on completion consumer
     * @return             this watcher
     */
    default Watcher watch(ByteSequence key, WatchOption option, Consumer<WatchResponse> onNext, Runnable onCompleted) {
        return watch(key, option, listener(onNext, t -> {
        }, onCompleted));
    }

    /**
     * Watch key with option.
     *
     * @param  key         key to be watched on.
     * @param  option      the options
     * @param  onNext      the on next consumer
     * @param  onError     the on error consumer
     * @param  onCompleted the on completion consumer
     * @return             this watcher
     */
    default Watcher watch(ByteSequence key, WatchOption option, Consumer<WatchResponse> onNext, Consumer<Throwable> onError,
        Runnable onCompleted) {
        return watch(key, option, listener(onNext, onError, onCompleted));
    }

    /**
     * Requests the latest revision processed for all watcher instances
     */
    void requestProgress();

    /**
     * Creates a listener with only onNext callback.
     *
     * @param  onNext the callback for watch responses
     * @return        the listener
     */
    static Listener listener(Consumer<WatchResponse> onNext) {
        return listener(onNext, t -> {
        }, () -> {
        });
    }

    /**
     * Creates a listener with onNext and onError callbacks.
     *
     * @param  onNext  the callback for watch responses
     * @param  onError the callback for errors
     * @return         the listener
     */
    static Listener listener(Consumer<WatchResponse> onNext, Consumer<Throwable> onError) {
        return listener(onNext, onError, () -> {
        });
    }

    /**
     * Creates a listener with onNext and onCompleted callbacks.
     *
     * @param  onNext      the callback for watch responses
     * @param  onCompleted the callback when completed
     * @return             the listener
     */
    static Listener listener(Consumer<WatchResponse> onNext, Runnable onCompleted) {
        return listener(onNext, t -> {
        }, onCompleted);
    }

    /**
     * Creates a listener with all callbacks.
     *
     * @param  onNext      the callback for watch responses
     * @param  onError     the callback for errors
     * @param  onCompleted the callback when completed
     * @return             the listener
     */
    static Listener listener(Consumer<WatchResponse> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        return new Listener() {
            @Override
            public void onNext(WatchResponse response) {
                onNext.accept(response);
            }

            @Override
            public void onError(Throwable throwable) {
                onError.accept(throwable);
            }

            @Override
            public void onCompleted() {
                onCompleted.run();
            }
        };
    }

    /**
     * Listener for watch events. Calls for each watcher run serially on Vert.x workers.
     */
    interface Listener {
        /**
         * Invoked on new events.
         *
         * @param response the response.
         */
        void onNext(WatchResponse response);

        /**
         * Invoked on errors.
         *
         * @param throwable the error.
         */
        void onError(Throwable throwable);

        /**
         * Invoked on completion.
         */
        void onCompleted();

        /**
         * Invoked when a retry attempt is about to occur.
         * This allows applications to monitor retry behavior and implement custom handling.
         *
         * @param context information about the retry attempt
         */
        default void onRetry(RetryContext context) {
            // Default no-op implementation for backward compatibility
        }

        /**
         * Invoked when the watcher's connection state changes.
         * This allows applications to monitor the watcher lifecycle.
         *
         * @param oldState the previous state
         * @param newState the new state
         */
        default void onStateChange(WatchState oldState, WatchState newState) {
            // Default no-op implementation for backward compatibility
        }
    }

    /**
     * Watcher interface for watching etcd keys.
     */
    interface Watcher extends Closeable {
        /**
         * Asynchronously closes this watcher's transport. The listener's completion callback is delivered
         * in order after any callbacks already queued for the watcher.
         *
         * @return CompletableFuture that completes when the transport closes
         */
        CompletableFuture<Void> closeAsync();

        /**
         * Synchronously closes this watcher and all its resources.
         * This is a blocking operation that delegates to {@link #closeAsync()} with a timeout.
         */
        @Override
        default void close() {
            try {
                closeAsync().get(10, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                // Log warning but don't throw - best effort cleanup
            } catch (Exception e) {
                // Wrap in unchecked exception
                throw new RuntimeException("Failed to close watcher", e);
            }
        }

        /**
         * Returns if watcher is already closed.
         *
         * @return true if closed
         */
        boolean isClosed();

        /**
         * Requests the latest revision processed and propagates it to listeners
         */
        void requestProgress();
    }
}
