package com.coreos.jetcd.watch;

import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.KeyValue;
import java.util.List;
import java.util.stream.Collectors;

public class WatchResponse extends AbstractResponse<com.coreos.jetcd.api.WatchResponse> {

  private List<WatchEvent> events;

  public WatchResponse(com.coreos.jetcd.api.WatchResponse response) {
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
