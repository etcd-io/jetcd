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

import io.etcd.jetcd.KeyValue;

/**
 * Watch event returned by watch, containing put or delete event information.
 *
 * @param keyValue  the key-value pair for the event
 * @param prevKV    the previous key-value pair (may be null)
 * @param eventType the type of event (PUT, DELETE, or UNRECOGNIZED)
 */
public record WatchEvent(KeyValue keyValue, KeyValue prevKV, EventType eventType) {

    /**
     * Watch event type.
     */
    public enum EventType {
        /** Put event. */
        PUT,
        /** Delete event. */
        DELETE,
        /** Unrecognized event type. */
        UNRECOGNIZED,
    }
}
