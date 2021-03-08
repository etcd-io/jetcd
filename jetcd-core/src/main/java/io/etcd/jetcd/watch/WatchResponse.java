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
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.api.Event;

public class WatchResponse extends AbstractResponse<io.etcd.jetcd.api.WatchResponse> {

    private List<WatchEvent> events;
    private final ByteSequence namespace;

    public WatchResponse(io.etcd.jetcd.api.WatchResponse response, ByteSequence namespace) {
        super(response, response.getHeader());
        this.namespace = namespace;
    }

    @VisibleForTesting
    public WatchResponse(io.etcd.jetcd.api.WatchResponse response) {
        this(response, ByteSequence.EMPTY);
    }

    /**
     * convert API watch event to client event.
     */
    private static WatchEvent toEvent(Event event, ByteSequence namespace) {
        WatchEvent.EventType eventType;
        switch (event.getType()) {
            case DELETE:
                eventType = WatchEvent.EventType.DELETE;
                break;
            case PUT:
                eventType = WatchEvent.EventType.PUT;
                break;
            default:
                eventType = WatchEvent.EventType.UNRECOGNIZED;
        }

        return new WatchEvent(new KeyValue(event.getKv(), namespace), new KeyValue(event.getPrevKv(), namespace), eventType);
    }

    public synchronized List<WatchEvent> getEvents() {
        if (events == null) {
            events = getResponse().getEventsList().stream().map(event -> WatchResponse.toEvent(event, namespace))
                .collect(Collectors.toList());
        }

        return events;
    }
}
