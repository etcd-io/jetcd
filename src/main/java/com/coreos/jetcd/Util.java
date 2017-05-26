package com.coreos.jetcd;

import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.api.ResponseHeader;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;

/**
 * This util is to convert api class to client class.
 */
class Util {

  private Util() {
  }

  /**
   * convert ByteSequence to ByteString.
   */
  protected static ByteString byteStringFromByteSequence(ByteSequence byteSequence) {
    return ByteString.copyFrom(byteSequence.getBytes());
  }

  /**
   * convert ByteString to ByteSequence.
   */
  protected static ByteSequence byteSequenceFromByteString(ByteString byteString) {
    return ByteSequence.fromBytes(byteString.toByteArray());
  }

  /**
   * convert API KeyValue to etcd client KeyValue.
   */
  protected static KeyValue apiToClientKV(com.coreos.jetcd.api.KeyValue keyValue) {
    return new KeyValue(
        byteSequenceFromByteString(keyValue.getKey()),
        byteSequenceFromByteString(keyValue.getValue()),
        keyValue.getCreateRevision(),
        keyValue.getModRevision(),
        keyValue.getVersion(),
        keyValue.getLease());
  }

  /**
   * convert API watch event to etcd client event.
   */
  protected static WatchEvent apiToClientEvent(Event event) {
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
    return new WatchEvent(apiToClientKV(event.getKv()), apiToClientKV(event.getPrevKv()),
        eventType);
  }

  protected static List<WatchEvent> apiToClientEvents(List<Event> events) {
    List<WatchEvent> watchEvents = new ArrayList<>();
    for (Event event : events) {
      watchEvents.add(apiToClientEvent(event));
    }
    return watchEvents;
  }

  /**
   * convert API response header to self defined header.
   */
  protected static Header apiToClientHeader(ResponseHeader header, long compactRevision) {
    return new Header(header.getClusterId(), header.getMemberId(), header.getRevision(),
        header.getRaftTerm(), compactRevision);
  }
}
