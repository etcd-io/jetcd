package com.coreos.jetcd.op;

import com.coreos.jetcd.api.DeleteRangeRequest;
import com.coreos.jetcd.api.PutRequest;
import com.coreos.jetcd.api.RangeRequest;
import com.coreos.jetcd.api.RequestOp;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.protobuf.ByteString;

/**
 * Etcd Operation.
 */
public abstract class Op {

  /**
   * Operation type.
   */
  public enum Type {
    PUT, RANGE, DELETE_RANGE,
  }

  protected final Type type;
  protected final ByteString key;

  protected Op(Type type, ByteString key) {
    this.type = type;
    this.key = key;
  }

  abstract RequestOp toRequestOp();

  public static PutOp put(ByteSequence key, ByteSequence value, PutOption option) {
    return new PutOp(ByteString.copyFrom(key.getBytes()), ByteString.copyFrom(value.getBytes()),
        option);
  }

  public static GetOp get(ByteSequence key, GetOption option) {
    return new GetOp(ByteString.copyFrom(key.getBytes()), option);
  }

  public static DeleteOp delete(ByteSequence key, DeleteOption option) {
    return new DeleteOp(ByteString.copyFrom(key.getBytes()), option);
  }

  public static final class PutOp extends Op {

    private final ByteString value;
    private final PutOption option;

    protected PutOp(ByteString key, ByteString value, PutOption option) {
      super(Type.PUT, key);
      this.value = value;
      this.option = option;
    }

    RequestOp toRequestOp() {
      PutRequest put = PutRequest.newBuilder()
          .setKey(this.key)
          .setValue(this.value)
          .setLease(this.option.getLeaseId())
          .setPrevKv(this.option.getPrevKV())
          .build();

      return RequestOp.newBuilder().setRequestPut(put).build();
    }
  }

  public static final class GetOp extends Op {

    private final GetOption option;

    protected GetOp(ByteString key, GetOption option) {
      super(Type.RANGE, key);
      this.option = option;
    }

    RequestOp toRequestOp() {
      RangeRequest.Builder range = RangeRequest.newBuilder()
          .setKey(this.key)
          .setCountOnly(this.option.isCountOnly())
          .setLimit(this.option.getLimit())
          .setRevision(this.option.getRevision())
          .setKeysOnly(this.option.isKeysOnly())
          .setSerializable(this.option.isSerializable())
          .setSortOrder(this.option.getSortOrder())
          .setSortTarget(this.option.getSortField());

      if (this.option.getEndKey().isPresent()) {
        range.setRangeEnd(ByteString.copyFrom(this.option.getEndKey().get().getBytes()));
      }

      return RequestOp.newBuilder().setRequestRange(range).build();
    }
  }

  public static final class DeleteOp extends Op {

    private final DeleteOption option;

    protected DeleteOp(ByteString key, DeleteOption option) {
      super(Type.DELETE_RANGE, key);
      this.option = option;
    }

    RequestOp toRequestOp() {
      DeleteRangeRequest.Builder delete = DeleteRangeRequest.newBuilder()
          .setKey(this.key)
          .setPrevKv(this.option.isPrevKV());

      if (this.option.getEndKey().isPresent()) {
        delete.setRangeEnd(ByteString.copyFrom(this.option.getEndKey().get().getBytes()));
      }

      return RequestOp.newBuilder().setRequestDeleteRange(delete).build();
    }
  }
}