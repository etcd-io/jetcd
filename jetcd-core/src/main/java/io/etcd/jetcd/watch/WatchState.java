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

package io.etcd.jetcd.watch;

/**
 * Represents the state of a watcher's connection lifecycle.
 *
 * <p>
 * This sealed interface ensures exhaustive handling of all watch states
 * and allows for state-specific data to be attached in the future.
 */
public sealed interface WatchState {

    /**
     * Initial state - connecting to gRPC stream.
     */
    record Connecting() implements WatchState {
    }

    /**
     * Stream connected - subscribe request sent, awaiting confirmation.
     */
    record Subscribing() implements WatchState {
    }

    /**
     * Watch active - receiving events.
     */
    record Watching() implements WatchState {
    }

    /**
     * Connection lost - attempting to reconnect.
     */
    record Reconnecting() implements WatchState {
    }

    /**
     * Terminal state - watcher closed.
     */
    record Closed() implements WatchState {
    }

    /** Connecting state. */
    WatchState CONNECTING = new Connecting();
    /** Subscribing state. */
    WatchState SUBSCRIBING = new Subscribing();
    /** Watching state. */
    WatchState WATCHING = new Watching();
    /** Reconnecting state. */
    WatchState RECONNECTING = new Reconnecting();
    /** Closed state. */
    WatchState CLOSED = new Closed();
}
