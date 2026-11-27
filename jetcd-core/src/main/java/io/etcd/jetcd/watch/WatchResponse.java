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

import java.util.List;
import java.util.function.Supplier;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.common.suppliers.Suppliers;
import io.etcd.jetcd.impl.AbstractResponse;

/**
 * Response from a watch operation.
 */
public class WatchResponse extends AbstractResponse<io.etcd.jetcd.api.WatchResponse> {

    private final Supplier<List<WatchEvent>> events;

    /**
     * Creates a new WatchResponse with namespace.
     *
     * @param response  the gRPC watch response
     * @param namespace the namespace
     */
    public WatchResponse(io.etcd.jetcd.api.WatchResponse response, ByteSequence namespace) {
        super(response, response.getHeader());

        this.events = Suppliers.memoizing(
            () -> getResponse().getEventsList().stream()
                .map(event -> toEvent(event, namespace))
                .toList());
    }

    /**
     * Creates a new WatchResponse without namespace.
     *
     * @param response the gRPC watch response
     */
    public WatchResponse(io.etcd.jetcd.api.WatchResponse response) {
        this(response, ByteSequence.EMPTY);
    }

    /**
     * convert API watch event to client event.
     */
    private static WatchEvent toEvent(Event event, ByteSequence namespace) {
        WatchEvent.EventType eventType = switch (event.getType()) {
            case DELETE -> WatchEvent.EventType.DELETE;
            case PUT -> WatchEvent.EventType.PUT;
            default -> WatchEvent.EventType.UNRECOGNIZED;
        };

        return new WatchEvent(new KeyValue(event.getKv(), namespace), new KeyValue(event.getPrevKv(), namespace), eventType);
    }

    /**
     * Returns the list of watch events.
     *
     * @return the watch events
     */
    public List<WatchEvent> getEvents() {
        return events.get();
    }

    /**
     * Returns true if the WatchResponse is progress notification.
     *
     * @return true if the WatchResponse is progress notification.
     */
    public boolean isProgressNotify() {
        return isProgressNotify(getResponse());
    }

    /**
     * Returns true if the WatchResponse is progress notification.
     *
     * @param  response the response.
     * @return          true if the WatchResponse is progress notification.
     */
    public static boolean isProgressNotify(io.etcd.jetcd.api.WatchResponse response) {
        return response.getEventsCount() == 0 && !response.getCreated() && !response.getCanceled()
            && response.getCompactRevision() == 0 && response.getHeader().getRevision() != 0;
    }

    /**
     * Returns true if the WatchResponse is created notification.
     *
     * @return true if the WatchResponse is created notification.
     */
    public boolean isCreatedNotify() {
        return isCreatedNotify(getResponse());
    }

    /**
     * Returns true if the WatchResponse is created notification.
     *
     * @param  response the response.
     * @return          true if the WatchResponse is created notification.
     */
    public static boolean isCreatedNotify(io.etcd.jetcd.api.WatchResponse response) {
        return response.getCreated() && !response.getCanceled();
    }
}
