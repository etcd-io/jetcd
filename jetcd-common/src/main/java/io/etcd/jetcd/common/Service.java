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

/**
 * Abstract base class for services that require lifecycle management.
 * Provides thread-safe start/stop operations with idempotency guarantees.
 * Subclasses implement {@link #doStart()} and {@link #doStop()} for actual lifecycle logic.
 */
public abstract class Service implements AutoCloseable {
    private final AtomicBoolean running;

    /**
     * Creates a new service instance.
     */
    protected Service() {
        this.running = new AtomicBoolean();
    }

    /**
     * Starts the service if it is not already running.
     * This operation is thread-safe and idempotent - calling start multiple times
     * will only execute {@link #doStart()} once until the service is stopped.
     */
    public void start() {
        if (this.running.compareAndSet(false, true)) {
            doStart();
        }
    }

    /**
     * Stops the service if it is currently running.
     * This operation is thread-safe and idempotent - calling stop multiple times
     * will only execute {@link #doStop()} once.
     */
    public void stop() {
        if (this.running.compareAndSet(true, false)) {
            doStop();
        }
    }

    /**
     * Restarts the service by stopping and then starting it.
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * Closes the service by stopping it.
     * Implements {@link AutoCloseable} to enable try-with-resources usage.
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * Checks if the service is currently running.
     *
     * @return true if the service is running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Extension point for subclasses to implement service start logic.
     * Called once when the service transitions from stopped to running state.
     */
    protected abstract void doStart();

    /**
     * Extension point for subclasses to implement service stop logic.
     * Called once when the service transitions from running to stopped state.
     */
    protected abstract void doStop();
}
