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

package io.etcd.jetcd.support;

import java.util.Objects;
import java.util.function.Supplier;

public class MemoizingClientSupplier<T extends CloseableClient> implements Supplier<T>, CloseableClient {
    final Supplier<T> delegate;
    transient volatile boolean initialized;
    transient T value;

    public MemoizingClientSupplier(Supplier<T> delegate) {
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
        return "Suppliers.memoize("
            + (initialized ? "<supplier that returned " + value + ">" : delegate)
            + ")";
    }

    @Override
    public void close() {
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
