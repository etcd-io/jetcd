package com.coreos.jetcd;

import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.api.ResponseHeader;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.lease.LeaseRevokeResponse;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.watch.WatchEvent;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * a util class for jetcd.
 */
class Util {

  private Util() {
  }

  /**
   * convert ByteSequence to ByteString.
   */
  static ByteString byteStringFromByteSequence(ByteSequence byteSequence) {
    return ByteString.copyFrom(byteSequence.getBytes());
  }

  /**
   * convert ByteString to ByteSequence.
   */
  static ByteSequence byteSequenceFromByteString(ByteString byteString) {
    return ByteSequence.fromBytes(byteString.toByteArray());
  }

  /**
   * convert API KeyValue to client KeyValue.
   */
  static KeyValue toKV(com.coreos.jetcd.api.KeyValue keyValue) {
    return new KeyValue(
        byteSequenceFromByteString(keyValue.getKey()),
        byteSequenceFromByteString(keyValue.getValue()),
        keyValue.getCreateRevision(),
        keyValue.getModRevision(),
        keyValue.getVersion(),
        keyValue.getLease());
  }

  /**
   * convert API watch event to client event.
   */
  static WatchEvent toEvent(Event event) {
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
    return new WatchEvent(toKV(event.getKv()), toKV(event.getPrevKv()),
        eventType);
  }

  /**
   * convert API events to client events.
   */
  static List<WatchEvent> toEvents(List<Event> events) {
    List<WatchEvent> watchEvents = new ArrayList<>();
    for (Event event : events) {
      watchEvents.add(toEvent(event));
    }
    return watchEvents;
  }

  /**
   * convert API response header to client header.
   */
  static Header toHeader(ResponseHeader header, long compactRevision) {
    return new Header(header.getClusterId(), header.getMemberId(), header.getRevision(),
        header.getRaftTerm(), compactRevision);
  }

  /**
   * convert API LeaseTimeToLiveResponse to client LeaseTimeToLiveResponse.
   */
  static LeaseTimeToLiveResponse toLeaseTimeToLiveResponse(
      com.coreos.jetcd.api.LeaseTimeToLiveResponse response) {
    List<ByteSequence> byteSequenceKeys = response.getKeysList().stream()
        .map(byteStringKey -> ByteSequence.fromBytes(byteStringKey.toByteArray()))
        .collect(Collectors.toList());
    return new LeaseTimeToLiveResponse(toHeader(response.getHeader(), 0),
        response.getID(), response.getTTL(), response.getGrantedTTL(), byteSequenceKeys);
  }

  /**
   * convert API LeaseGrantResponse to client LeaseGrantResponse.
   */
  static LeaseGrantResponse toLeaseGrantResponse(
      com.coreos.jetcd.api.LeaseGrantResponse response) {
    return new LeaseGrantResponse(toHeader(response.getHeader(), 0), response.getID(),
        response.getTTL());
  }

  /**
   * convert API LeaseRevokeResponse to client LeaseRevokeResponse.
   */
  static LeaseRevokeResponse toLeaseRevokeResponse(
      com.coreos.jetcd.api.LeaseRevokeResponse response) {
    return new LeaseRevokeResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API LeaseKeepAliveResponse to client LeaseKeepAliveResponse.
   */
  static LeaseKeepAliveResponse toLeaseKeepAliveResponse(
      com.coreos.jetcd.api.LeaseKeepAliveResponse response) {
    return new com.coreos.jetcd.lease.LeaseKeepAliveResponse(toHeader(response.getHeader(), 0),
        response.getID(),
        response.getTTL());
  }

  /**
   * convert ListenableFuture of Type S to CompletableFuture of Type T.
   */
  static <S, T> CompletableFuture<T> listenableToCompletableFuture(
      final ListenableFuture<S> sourceFuture, final FutureResultConvert<S, T> resultConvert,
      Executor executor) {
    CompletableFuture<T> targetFuture = new CompletableFuture<T>() {
      // the cancel of targetFuture also cancels the sourceFuture.
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        return sourceFuture.cancel(mayInterruptIfRunning);
      }
    };

    sourceFuture.addListener(() -> {
      try {
        targetFuture.complete(resultConvert.convert(sourceFuture.get()));
      } catch (Exception e) {
        targetFuture.completeExceptionally(e);
      }
    }, executor);

    return targetFuture;
  }

  @FunctionalInterface
  interface FutureResultConvert<S, T> {

    T convert(S source);
  }
}
