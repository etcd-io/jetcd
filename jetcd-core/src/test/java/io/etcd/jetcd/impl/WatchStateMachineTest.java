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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WatchStateMachine state transitions.
 */
@Tag("watch")
class WatchStateMachineTest {

    private TestHandler handler;
    private WatchStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        stateMachine = new WatchStateMachine(handler);
    }

    @Test
    void testHappyPath() {
        // Initial state
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Connecting.class);

        // Start
        stateMachine.start();
        assertThat(handler.events).containsExactly("onConnect");
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Connecting.class);

        // Stream ready
        handler.clear();
        stateMachine.streamReady();
        assertThat(handler.events).containsExactly("onStateChange", "onSubscribe");
        assertThat(handler.lastOldState).isInstanceOf(WatchStateMachine.State.Connecting.class);
        assertThat(handler.lastNewState).isInstanceOf(WatchStateMachine.State.Subscribing.class);
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Subscribing.class);

        // Watch created
        handler.clear();
        stateMachine.watchCreated();
        assertThat(handler.events).containsExactly("onStateChange", "onReady");
        assertThat(handler.lastOldState).isInstanceOf(WatchStateMachine.State.Subscribing.class);
        assertThat(handler.lastNewState).isInstanceOf(WatchStateMachine.State.Watching.class);
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Watching.class);
        assertThat(stateMachine.isWatching()).isTrue();
    }

    @Test
    void testReconnectionFlow() {
        // Get to WATCHING state
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        handler.clear();

        // Stream ends
        stateMachine.streamEnded();
        assertThat(handler.events).containsExactly("onStateChange", "onReconnect");
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Reconnecting.class);

        // Reconnect succeeds
        handler.clear();
        stateMachine.reconnectSucceeded();
        assertThat(handler.events).containsExactly("onStateChange", "onConnect");
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Connecting.class);
    }

    @Test
    void testStreamErrorTriggersReconnect() {
        // Get to WATCHING state
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        handler.clear();

        // Stream error
        RuntimeException error = new RuntimeException("connection lost");
        stateMachine.streamError(error);
        assertThat(handler.events).containsExactly("onError", "onStateChange", "onReconnect");
        assertThat(handler.lastError).isEqualTo(error);
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Reconnecting.class);
    }

    @Test
    void testReconnectFailed() {
        // Get to RECONNECTING state
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        stateMachine.streamEnded();
        handler.clear();

        // Reconnect fails
        RuntimeException error = new RuntimeException("max retries exceeded");
        stateMachine.reconnectFailed(error);
        assertThat(handler.events).containsExactly("onStateChange", "onError", "onClose");
        assertThat(handler.lastError).isEqualTo(error);
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Closed.class);
        assertThat(stateMachine.isClosed()).isTrue();
    }

    @Test
    void testWatchCanceled() {
        // Get to WATCHING state
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        handler.clear();

        // Watch canceled by server
        RuntimeException error = new RuntimeException("watch canceled");
        stateMachine.watchCanceled(error);
        assertThat(handler.events).containsExactly("onStateChange", "onError", "onClose");
        assertThat(handler.lastError).isEqualTo(error);
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Closed.class);
    }

    @Test
    void testCloseFromConnecting() {
        stateMachine.start();
        handler.clear();

        stateMachine.close();
        assertThat(handler.events).containsExactly("onStateChange", "onClose");
        assertThat(stateMachine.isClosed()).isTrue();
    }

    @Test
    void testCloseFromWatching() {
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        handler.clear();

        stateMachine.close();
        assertThat(handler.events).containsExactly("onStateChange", "onClose");
        assertThat(stateMachine.isClosed()).isTrue();
    }

    @Test
    void testCloseFromReconnecting() {
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        stateMachine.streamEnded();
        handler.clear();

        stateMachine.close();
        assertThat(handler.events).containsExactly("onStateChange", "onClose");
        assertThat(stateMachine.isClosed()).isTrue();
    }

    @Test
    void testDoubleCloseIsIgnored() {
        stateMachine.start();
        stateMachine.close();
        handler.clear();

        stateMachine.close();
        assertThat(handler.events).isEmpty();
    }

    @Test
    void testInvalidStateTransitionStreamReady() {
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        handler.clear();

        // streamReady in WATCHING state is invalid
        stateMachine.streamReady();
        assertThat(handler.events).containsExactly("onError");
        assertThat(handler.lastError).isInstanceOf(IllegalStateException.class);
        assertThat(handler.lastError.getMessage()).contains("streamReady()").contains("WATCHING");
    }

    @Test
    void testInvalidStateTransitionWatchCreated() {
        stateMachine.start();
        handler.clear();

        // watchCreated in CONNECTING state is invalid
        stateMachine.watchCreated();
        assertThat(handler.events).containsExactly("onError");
        assertThat(handler.lastError).isInstanceOf(IllegalStateException.class);
        assertThat(handler.lastError.getMessage()).contains("watchCreated()").contains("CONNECTING");
    }

    @Test
    void testInvalidStateTransitionReconnectSucceeded() {
        stateMachine.start();
        stateMachine.streamReady();
        stateMachine.watchCreated();
        handler.clear();

        // reconnectSucceeded in WATCHING state is invalid
        stateMachine.reconnectSucceeded();
        assertThat(handler.events).containsExactly("onError");
        assertThat(handler.lastError).isInstanceOf(IllegalStateException.class);
        assertThat(handler.lastError.getMessage()).contains("reconnectSucceeded()").contains("WATCHING");
    }

    @Test
    void testEventsIgnoredWhenClosed() {
        stateMachine.start();
        stateMachine.close();
        handler.clear();

        // All these should be ignored when closed
        stateMachine.streamReady();
        stateMachine.watchCreated();
        stateMachine.streamEnded();
        stateMachine.streamError(new RuntimeException());
        stateMachine.reconnectSucceeded();
        stateMachine.reconnectFailed(new RuntimeException());

        assertThat(handler.events).isEmpty();
    }

    @Test
    void testStreamEndedFromConnecting() {
        stateMachine.start();
        handler.clear();

        // Stream can end before fully connected
        stateMachine.streamEnded();
        assertThat(handler.events).containsExactly("onStateChange", "onReconnect");
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Reconnecting.class);
    }

    @Test
    void testStreamEndedFromSubscribing() {
        stateMachine.start();
        stateMachine.streamReady();
        handler.clear();

        stateMachine.streamEnded();
        assertThat(handler.events).containsExactly("onStateChange", "onReconnect");
        assertThat(stateMachine.currentState()).isInstanceOf(WatchStateMachine.State.Reconnecting.class);
    }

    /**
     * Test handler that records events for verification.
     */
    private static class TestHandler implements WatchStateMachine.Handler {
        final List<String> events = new ArrayList<>();
        volatile Throwable lastError;
        volatile WatchStateMachine.State lastOldState;
        volatile WatchStateMachine.State lastNewState;

        void clear() {
            events.clear();
            lastError = null;
            lastOldState = null;
            lastNewState = null;
        }

        @Override
        public void onConnect() {
            events.add("onConnect");
        }

        @Override
        public void onSubscribe() {
            events.add("onSubscribe");
        }

        @Override
        public void onReady() {
            events.add("onReady");
        }

        @Override
        public void onReconnect() {
            events.add("onReconnect");
        }

        @Override
        public void onClose() {
            events.add("onClose");
        }

        @Override
        public void onError(Throwable error) {
            events.add("onError");
            lastError = error;
        }

        @Override
        public void onStateChange(WatchStateMachine.State oldState, WatchStateMachine.State newState) {
            events.add("onStateChange");
            lastOldState = oldState;
            lastNewState = newState;
        }
    }
}
