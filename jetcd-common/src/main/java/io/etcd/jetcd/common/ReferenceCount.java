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

package io.etcd.jetcd.common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe reference counter with callbacks.
 * Inspired by Apache Camel's ReferenceCount implementation.
 *
 * @see <a href=
 *      "https://github.com/apache/camel/blob/main/core/camel-util/src/main/java/org/apache/camel/util/ReferenceCount.java">Apache
 *      Camel ReferenceCount</a>
 */
public final class ReferenceCount {
    private final AtomicLong count;
    private final Runnable onFirst;
    private final Runnable onRelease;

    private ReferenceCount(Runnable onFirst, Runnable onRelease) {
        this.count = new AtomicLong(0);
        this.onFirst = Preconditions.requireNonNull(onFirst, "onFirst");
        this.onRelease = Preconditions.requireNonNull(onRelease, "onRelease");
    }

    /**
     * Returns the current reference count.
     *
     * @return the reference count
     */
    public long get() {
        return count.get();
    }

    /**
     * Increments the reference count and invokes onFirst callback when going from 0 to 1.
     *
     * @throws IllegalStateException if the reference count has already been released (negative)
     */
    public void retain() {
        while (true) {
            long v = count.get();
            if (v < 0) {
                throw new IllegalStateException("ReferenceCount already released");
            }

            if (count.compareAndSet(v, v + 1)) {
                if (v == 0) {
                    onFirst.run();
                }
                break;
            }
        }
    }

    /**
     * Decrements the reference count and invokes onRelease callback when going from 1 to 0.
     *
     * @throws IllegalStateException if the reference count is already at zero
     */
    public void release() {
        while (true) {
            long v = count.get();
            if (v <= 0) {
                throw new IllegalStateException("ReferenceCount already at zero");
            }

            if (count.compareAndSet(v, v - 1)) {
                if (v == 1) {
                    onRelease.run();
                }
                break;
            }
        }
    }

    /**
     * Creates a ReferenceCount with callbacks for first retain and last release.
     *
     * @param  onFirst   callback invoked when count goes from 0 to 1
     * @param  onRelease callback invoked when count goes from 1 to 0
     * @return           new ReferenceCount instance
     */
    public static ReferenceCount on(Runnable onFirst, Runnable onRelease) {
        return new ReferenceCount(onFirst, onRelease);
    }
}
