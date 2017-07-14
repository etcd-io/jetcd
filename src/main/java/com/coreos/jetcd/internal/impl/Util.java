package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.api.CompactionResponse;
import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.ResponseHeader;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.CompactResponse;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.kv.TxnResponse;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.lease.LeaseRevokeResponse;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.maintenance.AlarmMember;
import com.coreos.jetcd.maintenance.AlarmResponse;
import com.coreos.jetcd.maintenance.DefragmentResponse;
import com.coreos.jetcd.maintenance.StatusResponse;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * a util class for jetcd.
 */
final class Util {

  private static final Logger logger = Logger.getLogger(Util.class.getName());

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
   * convert API rangeResponse to client GetResponse.
   */
  static GetResponse toGetResponse(RangeResponse rangeResponse) {
    return new GetResponse(toHeader(rangeResponse.getHeader(), 0),
        toKVs(rangeResponse.getKvsList()), rangeResponse.getMore(),
        rangeResponse.getCount());
  }

  /**
   * convert API PutResponse to client PutResponse.
   */
  static PutResponse toPutResponse(com.coreos.jetcd.api.PutResponse response) {
    return new PutResponse(toHeader(response.getHeader(), 0), toKV(response.getPrevKv()),
        response.hasPrevKv());
  }

  /**
   * convert API DeleteRangeResponse to client DeleteResponse.
   */
  static DeleteResponse toDeleteResponse(DeleteRangeResponse rangeResponse) {
    return new DeleteResponse(toHeader(rangeResponse.getHeader(), 0),
        rangeResponse.getDeleted(),
        toKVs(rangeResponse.getPrevKvsList()));
  }

  private static List<KeyValue> toKVs(List<com.coreos.jetcd.api.KeyValue> kvs) {
    return kvs.stream().map(Util::toKV).collect(Collectors.toList());
  }

  /**
   * convert API CompactionResponse to client CompactResponse.
   */
  static CompactResponse toCompactResponse(CompactionResponse response) {
    return new CompactResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API WatchResponse to client WatchResponse.
   */
  static WatchResponse toWatchResponse(com.coreos.jetcd.api.WatchResponse response) {
    return new WatchResponse(toHeader(response.getHeader(), 0),
        toEvents(response.getEventsList()));
  }

  /**
   * convert API TxnResponse to client TxnResponse.
   */
  static TxnResponse toTxnResponse(com.coreos.jetcd.api.TxnResponse response) {
    List<PutResponse> putResponses = new ArrayList<>();
    List<GetResponse> getResponses = new ArrayList<>();
    List<DeleteResponse> deleteResponses = new ArrayList<>();

    response.getResponsesList().forEach((responseOp) -> {
      switch (responseOp.getResponseCase()) {
        case RESPONSE_PUT:
          putResponses.add(toPutResponse(responseOp.getResponsePut()));
          break;
        case RESPONSE_RANGE:
          getResponses.add(toGetResponse(responseOp.getResponseRange()));
          break;
        case RESPONSE_DELETE_RANGE:
          deleteResponses.add(toDeleteResponse(responseOp.getResponseDeleteRange()));
          break;
        default:
          logger.log(Level.WARNING, "unexpected type " + responseOp.getResponseCase());
      }
    });

    return new TxnResponse(
        toHeader(response.getHeader(), 0),
        response.getSucceeded(),
        putResponses,
        getResponses,
        deleteResponses);
  }

  /**
   * convert API StatusResponse to client StatusResponse.
   */
  static StatusResponse toStatusResponse(com.coreos.jetcd.api.StatusResponse response) {
    return new StatusResponse(
        toHeader(response.getHeader(), 0),
        response.getVersion(),
        response.getDbSize(),
        response.getLeader(),
        response.getRaftIndex(),
        response.getRaftTerm());
  }

  /**
   * convert API DefragmentResponse to client DefragmentResponse.
   */
  static DefragmentResponse toDefragmentResponse(
      com.coreos.jetcd.api.DefragmentResponse response) {
    return new DefragmentResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AlarmResponse to client AlarmResponse.
   */
  static AlarmResponse toAlarmResponse(com.coreos.jetcd.api.AlarmResponse response) {
    return new AlarmResponse(
        toHeader(response.getHeader(), 0),
        toAlarmMembers(response.getAlarmsList())
    );
  }

  /**
   * convert a list of API AlarmMember to a list of  client AlarmMember.
   */
  private static List<AlarmMember> toAlarmMembers(
      List<com.coreos.jetcd.api.AlarmMember> alarmMembers) {
    return alarmMembers.stream()
        .map(Util::toAlarmMember)
        .collect(Collectors.toList());
  }

  /**
   * convert API AlarmMember to a client AlarmMember.
   */
  private static AlarmMember toAlarmMember(com.coreos.jetcd.api.AlarmMember alarmMember) {
    com.coreos.jetcd.maintenance.AlarmType type;
    switch (alarmMember.getAlarm()) {
      case NONE:
        type = com.coreos.jetcd.maintenance.AlarmType.NONE;
        break;
      case NOSPACE:
        type = com.coreos.jetcd.maintenance.AlarmType.NOSPACE;
        break;
      default:
        type = com.coreos.jetcd.maintenance.AlarmType.UNRECOGNIZED;
    }
    return new AlarmMember(alarmMember.getMemberID(), type);
  }

  /**
   * convert ListenableFuture of Type S to CompletableFuture of Type T.
   */
  static <S, T> CompletableFuture<T> listenableToCompletableFuture(
      ListenableFuture<S> sourceFuture, Function<S, T> resultConvert, Executor executor) {
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
        targetFuture.complete(resultConvert.apply(sourceFuture.get()));
      } catch (Exception e) {
        targetFuture.completeExceptionally(e);
      }
    }, executor);

    return targetFuture;
  }
}
