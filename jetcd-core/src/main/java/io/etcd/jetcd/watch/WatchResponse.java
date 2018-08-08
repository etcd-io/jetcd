/**
 * Copyright 2017 The jetcd authors
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

import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.data.AbstractResponse;
import io.etcd.jetcd.data.KeyValue;
import java.util.List;
import java.util.stream.Collectors;

public class WatchResponse extends AbstractResponse<io.etcd.jetcd.api.WatchResponse> {

  private List<WatchEvent> events;

  public WatchResponse(io.etcd.jetcd.api.WatchResponse response) {
    super(response, response.getHeader());
  }

  /**
   * convert API watch event to client event.
   */
  private static WatchEvent toEvent(Event event) {
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

    return new WatchEvent(
        new KeyValue(event.getKv()),
        new KeyValue(event.getPrevKv()),
        eventType);
  }

  public synchronized List<WatchEvent> getEvents() {
    if (events == null) {
      events = getResponse().getEventsList().stream()
          .map(WatchResponse::toEvent).collect(
              Collectors.toList());

    }

    return events;
  }
}
