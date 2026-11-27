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

package io.etcd.jetcd.common.suppliers;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.vertx.core.Future;

/**
 * Utility class for creating specialized Supplier implementations.
 */
public final class Suppliers {

    private Suppliers() {
    }

    /**
     * Returns a supplier that caches the instance retrieved during the first call to {@code get()}
     * and returns that value on subsequent calls. The returned supplier is thread-safe.
     *
     * @param  delegate the underlying supplier
     * @param  <T>      the type of results supplied by this supplier
     * @return          a memoizing supplier
     */
    public static <T> Supplier<T> memoizing(Supplier<T> delegate) {
        return new MemorizingSupplier<>(delegate);
    }

    /**
     * Returns a supplier that caches the instance retrieved during the first call to {@code get()}
     * and returns that value on subsequent calls. The returned supplier is thread-safe and
     * implements {@link AutoCloseable} to properly clean up the cached resource.
     *
     * @param  delegate the underlying supplier
     * @param  <T>      the type of results supplied by this supplier
     * @return          a memoizing supplier that implements AutoCloseable
     */
    public static <T extends AutoCloseable> CloseableSupplier<T> memoizingCloseable(Supplier<T> delegate) {
        return new MemorizingCloseableSupplier<>(delegate);
    }

    /**
     * Returns a memoizing supplier that returns Optional.
     * The supplier may return null to indicate absence, which will be wrapped as Optional.empty().
     * The result is cached after the first invocation.
     *
     * @param  supplier function that computes the value (may return null)
     * @param  <T>      the type of the value
     * @return          Supplier that caches and returns Optional
     */
    public static <T> Supplier<Optional<T>> memoizingOptional(Supplier<T> supplier) {
        return new MemorizingOptionalSupplier<>(supplier);
    }

    /**
     * Returns a supplier that caches the instance retrieved during the first call to {@code get()}
     * and returns that value on subsequent calls. The returned supplier is thread-safe and
     * supports async close operations for types like Vert.x GrpcClient.
     *
     * @param  delegate the underlying supplier
     * @param  <T>      the type of results supplied by this supplier
     * @return          a memoizing supplier that supports async close
     */
    public static <T extends AsyncCloseable> AsyncCloseableSupplier<T> memoizingAsyncCloseable(Supplier<T> delegate) {
        return new MemorizingAsyncCloseableSupplier<>(delegate);
    }

    /**
     * A supplier that memoizes the result of another supplier and implements AutoCloseable
     * to properly clean up the cached resource.
     *
     * @param <T> the type of closeable results supplied
     */
    public static class MemorizingCloseableSupplier<T extends AutoCloseable> implements CloseableSupplier<T> {
        final Supplier<T> delegate;
        volatile boolean initialized;
        T value;

        MemorizingCloseableSupplier(Supplier<T> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return "Suppliers.memoizingCloseable("
                + (initialized ? "<supplier that returned " + value + ">" : delegate)
                + ")";
        }

        @Override
        public void close() throws Exception {
            if (initialized) {
                synchronized (this) {
                    if (initialized) {
                        if (value != null) {
                            value.close();
                            value = null;
                        }
                        initialized = false;
                    }
                }
            }
        }
    }

    /**
     * A supplier that memoizes the result of another supplier.
     *
     * @param <T> the type of results supplied
     */
    public static class MemorizingSupplier<T> implements Supplier<T> {
        final Supplier<T> delegate;
        volatile boolean initialized;
        T value;

        MemorizingSupplier(Supplier<T> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return "Suppliers.memoizing("
                + (initialized ? "<supplier that returned " + value + ">" : delegate)
                + ")";
        }
    }

    /**
     * A supplier that memoizes an Optional result.
     * The supplied value may be null, which is wrapped as Optional.empty().
     *
     * @param <T> the type of results supplied
     */
    public static class MemorizingOptionalSupplier<T> implements Supplier<Optional<T>> {
        private final Supplier<T> delegate;

        MemorizingOptionalSupplier(Supplier<T> supplier) {
            Objects.requireNonNull(supplier);
            this.delegate = memoizing(supplier);
        }

        @Override
        public Optional<T> get() {
            return Optional.ofNullable(delegate.get());
        }

        @Override
        public String toString() {
            return "Suppliers.memoizingOptional(...)";
        }
    }

    /**
     * A supplier that memoizes the result of another supplier and supports async close.
     * Thread-safe using double-checked locking pattern.
     *
     * @param <T> the type of async closeable results supplied
     */
    public static class MemorizingAsyncCloseableSupplier<T extends AsyncCloseable> implements AsyncCloseableSupplier<T> {
        private final Supplier<T> delegate;
        private volatile boolean initialized;
        private T value;

        MemorizingAsyncCloseableSupplier(Supplier<T> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public T get() {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public Future<Void> close() {
            if (initialized) {
                synchronized (this) {
                    if (initialized) {
                        T v = value;
                        value = null;
                        initialized = false;
                        if (v != null) {
                            return v.close();
                        }
                    }
                }
            }
            return Future.succeededFuture();
        }

        @Override
        public String toString() {
            return "Suppliers.memoizingAsyncCloseable("
                + (initialized ? "<supplier that returned " + value + ">" : delegate)
                + ")";
        }
    }
}
