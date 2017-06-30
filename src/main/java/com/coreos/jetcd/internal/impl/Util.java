package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.api.CompactionResponse;
import com.coreos.jetcd.api.DeleteRangeResponse;
import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.api.RangeResponse;
import com.coreos.jetcd.api.ResponseHeader;
import com.coreos.jetcd.auth.AuthDisableResponse;
import com.coreos.jetcd.auth.AuthEnableResponse;
import com.coreos.jetcd.auth.AuthRoleAddResponse;
import com.coreos.jetcd.auth.AuthRoleDeleteResponse;
import com.coreos.jetcd.auth.AuthRoleGetResponse;
import com.coreos.jetcd.auth.AuthRoleGrantPermissionResponse;
import com.coreos.jetcd.auth.AuthRoleListResponse;
import com.coreos.jetcd.auth.AuthRoleRevokePermissionResponse;
import com.coreos.jetcd.auth.AuthUserAddResponse;
import com.coreos.jetcd.auth.AuthUserChangePasswordResponse;
import com.coreos.jetcd.auth.AuthUserDeleteResponse;
import com.coreos.jetcd.auth.AuthUserGetResponse;
import com.coreos.jetcd.auth.AuthUserGrantRoleResponse;
import com.coreos.jetcd.auth.AuthUserListResponse;
import com.coreos.jetcd.auth.AuthUserRevokeRoleResponse;
import com.coreos.jetcd.auth.Permission;
import com.coreos.jetcd.auth.Permission.Type;
import com.coreos.jetcd.cluster.Member;
import com.coreos.jetcd.cluster.MemberAddResponse;
import com.coreos.jetcd.cluster.MemberListResponse;
import com.coreos.jetcd.cluster.MemberRemoveResponse;
import com.coreos.jetcd.cluster.MemberUpdateResponse;
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
   * <<<<<<< c21601f11678ca2f9a702addc9fcb04d837acee8
   * convert API MemberAddResponse to MemberAddResponse.
   */
  static MemberAddResponse toMemberAddResponse(com.coreos.jetcd.api.MemberAddResponse response) {
    return new MemberAddResponse(
        toHeader(response.getHeader(), 0),
        toMember(response.getMember()),
        toMembers(response.getMembersList())
    );
  }

  /**
   * convert API MemberListResponse to MemberListResponse.
   */
  static MemberListResponse toMemberListResponse(
      com.coreos.jetcd.api.MemberListResponse response) {
    return new MemberListResponse(
        toHeader(response.getHeader(), 0),
        toMembers(response.getMembersList())
    );
  }

  /**
   * convert API MemberRemoveResponse to MemberRemoveResponse.
   */
  static MemberRemoveResponse toMemberRemoveResponse(
      com.coreos.jetcd.api.MemberRemoveResponse response) {
    return new MemberRemoveResponse(
        toHeader(response.getHeader(), 0),
        toMembers(response.getMembersList())
    );
  }

  /**
   * convert API MemberUpdateResponse to MemberUpdateResponse.
   */
  static MemberUpdateResponse toMemberUpdateResponse(
      com.coreos.jetcd.api.MemberUpdateResponse response) {
    return new MemberUpdateResponse(
        toHeader(response.getHeader(), 0),
        toMembers(response.getMembersList())
    );
  }

  /**
   * convert API Members to Members.
   */
  private static List<Member> toMembers(List<com.coreos.jetcd.api.Member> members) {
    return members.stream()
        .map(Util::toMember)
        .collect(Collectors.toList());
  }

  /**
   * convert API Member to Member.
   */
  private static Member toMember(com.coreos.jetcd.api.Member member) {
    return new Member(
        member.getID(),
        member.getName(),
        member.getPeerURLsList(),
        member.getClientURLsList()
    );
  }

  /**
   * convert API AuthDisableResponse to client AuthDisableResponse.
   */
  static AuthDisableResponse toAuthDisableResponse(
      com.coreos.jetcd.api.AuthDisableResponse response) {
    return new AuthDisableResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthEnableResponse to client AuthEnableResponse.
   */
  static AuthEnableResponse toAuthEnableResponse(
      com.coreos.jetcd.api.AuthEnableResponse response) {
    return new AuthEnableResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthRoleAddResponse to client AuthRoleAddResponse.
   */
  static AuthRoleAddResponse toAuthRoleAddResponse(
      com.coreos.jetcd.api.AuthRoleAddResponse response) {
    return new AuthRoleAddResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthRoleDeleteResponse to client AuthRoleDeleteResponse.
   */
  static AuthRoleDeleteResponse toAuthRoleDeleteResponse(
      com.coreos.jetcd.api.AuthRoleDeleteResponse response) {
    return new AuthRoleDeleteResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthRoleGetResponse to client AuthRoleGetResponse.
   */
  static AuthRoleGetResponse toAuthRoleGetResponse(
      com.coreos.jetcd.api.AuthRoleGetResponse response) {
    List<Permission> permissions = response.getPermList()
        .stream()
        .map(Util::toPermission)
        .collect(Collectors.toList());
    return new AuthRoleGetResponse(toHeader(response.getHeader(), 0), permissions);
  }

  private static Permission toPermission(com.coreos.jetcd.api.Permission perm) {
    ByteSequence key = byteSequenceFromByteString(perm.getKey());
    ByteSequence rangeEnd = byteSequenceFromByteString(perm.getRangeEnd());
    Permission.Type type;
    switch (perm.getPermType()) {
      case READ:
        type = Type.READ;
        break;
      case WRITE:
        type = Type.WRITE;
        break;
      case READWRITE:
        type = Type.READWRITE;
        break;
      default:
        type = Type.UNRECOGNIZED;
    }
    return new Permission(type, key, rangeEnd);
  }

  /**
   * convert API AuthRoleGrantPermissionResponse to client AuthRoleGrantPermissionResponse.
   */
  static AuthRoleGrantPermissionResponse toAuthRoleGrantPermissionResponse(
      com.coreos.jetcd.api.AuthRoleGrantPermissionResponse response) {
    return new AuthRoleGrantPermissionResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthRoleListResponse to client AuthRoleListResponse.
   */
  static AuthRoleListResponse toAuthRoleListResponse(
      com.coreos.jetcd.api.AuthRoleListResponse response) {
    List<String> roles = new ArrayList<>(response.getRolesList());
    return new AuthRoleListResponse(toHeader(response.getHeader(), 0), roles);
  }

  /**
   * convert API AuthRoleRevokePermissionResponse to client AuthRoleRevokePermissionResponse.
   */
  static AuthRoleRevokePermissionResponse toAuthRoleRevokePermissionResponse(
      com.coreos.jetcd.api.AuthRoleRevokePermissionResponse response) {
    return new AuthRoleRevokePermissionResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthUserAddResponse to client AuthUserAddResponse.
   */
  static AuthUserAddResponse toAuthUserAddResponse(
      com.coreos.jetcd.api.AuthUserAddResponse response) {
    return new AuthUserAddResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthUserChangePasswordResponse to client AuthUserChangePasswordResponse.
   */
  static AuthUserChangePasswordResponse toAuthUserChangePasswordResponse(
      com.coreos.jetcd.api.AuthUserChangePasswordResponse response) {
    return new AuthUserChangePasswordResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthUserDeleteResponse to client AuthUserDeleteResponse.
   */
  static AuthUserDeleteResponse toAuthUserDeleteResponse(
      com.coreos.jetcd.api.AuthUserDeleteResponse response) {
    return new AuthUserDeleteResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthUserGetResponse to client AuthUserGetResponse.
   */
  static AuthUserGetResponse toAuthUserGetResponse(
      com.coreos.jetcd.api.AuthUserGetResponse response) {
    List<String> roles = new ArrayList<>(response.getRolesList());
    return new AuthUserGetResponse(toHeader(response.getHeader(), 0), roles);
  }

  /**
   * convert API AuthUserGrantRoleResponse to client AuthUserGrantRoleResponse.
   */
  static AuthUserGrantRoleResponse toAuthUserGrantRoleResponse(
      com.coreos.jetcd.api.AuthUserGrantRoleResponse response) {
    return new AuthUserGrantRoleResponse(toHeader(response.getHeader(), 0));
  }

  /**
   * convert API AuthUserListResponse to client AuthUserListResponse.
   */
  static AuthUserListResponse toAuthUserListResponse(
      com.coreos.jetcd.api.AuthUserListResponse response) {
    List<String> users = new ArrayList<>(response.getUsersList());
    return new AuthUserListResponse(toHeader(response.getHeader(), 0), users);
  }

  /**
   * convert API AuthUserRevokeRoleResponse to client AuthUserRevokeRoleResponse.
   */
  static AuthUserRevokeRoleResponse toAuthUserRevokeRoleResponse(
      com.coreos.jetcd.api.AuthUserRevokeRoleResponse response) {
    return new AuthUserRevokeRoleResponse(toHeader(response.getHeader(), 0));
  }


  /**
   * >>>>>>> util: add convert methods for Auth responses and add UNRECOGNIZED to Permission
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
