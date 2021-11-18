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

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Service implements AutoCloseable {
    private final AtomicBoolean running;

    protected Service() {
        this.running = new AtomicBoolean();
    }

    public void start() {
        if (this.running.compareAndSet(false, true)) {
            doStart();
        }
    }

    public void stop() {
        if (this.running.compareAndSet(true, false)) {
            doStop();
        }
    }

    public void restart() {
        stop();
        start();
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        return running.get();
    }

    protected abstract void doStart();

    protected abstract void doStop();
}
