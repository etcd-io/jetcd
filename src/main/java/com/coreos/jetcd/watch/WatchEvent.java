package com.coreos.jetcd.watch;

import com.coreos.jetcd.data.KeyValue;

/**
 * Watch event, return by watch, contain put, delete event.
 */
public class WatchEvent {

  public enum EventType {
    PUT,
    DELETE,
    UNRECOGNIZED,
  }

  private final KeyValue keyValue;

  private final KeyValue prevKV;

  private final EventType eventType;

  public WatchEvent(KeyValue keyValue, KeyValue prevKV, EventType eventType) {
    this.keyValue = keyValue;
    this.prevKV = prevKV;
    this.eventType = eventType;
  }

  public KeyValue getKeyValue() {
    return keyValue;
  }

  public KeyValue getPrevKV() {
    return prevKV;
  }

  public EventType getEventType() {
    return eventType;
  }
}
