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

import io.etcd.jetcd.KeyValue;

/**
 * Watch event, return by watch, contain put, delete event.
 */
public class WatchEvent {

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

  public enum EventType {
    PUT,
    DELETE,
    UNRECOGNIZED,
  }
}
