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

package io.etcd.jetcd.op;

import static io.etcd.jetcd.options.OptionsUtil.toRangeRequestSortOrder;
import static io.etcd.jetcd.options.OptionsUtil.toRangeRequestSortTarget;

import com.google.protobuf.ByteString;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Util;
import io.etcd.jetcd.api.DeleteRangeRequest;
import io.etcd.jetcd.api.PutRequest;
import io.etcd.jetcd.api.RangeRequest;
import io.etcd.jetcd.api.RequestOp;
import io.etcd.jetcd.api.TxnRequest;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;

/**
 * Etcd Operation.
 */
public abstract class Op {

  /**
   * Operation type.
   */
  public enum Type {
    PUT, RANGE, DELETE_RANGE, TXN
  }

  protected final Type type;
  protected final ByteString key;

  protected Op(Type type, ByteString key) {
    this.type = type;
    this.key = key;
  }

  abstract RequestOp toRequestOp(ByteSequence namespace);

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

  public static TxnOp txn(Cmp[] cmps, Op[] thenOps, Op[] elseOps) {
    return new TxnOp(cmps, thenOps, elseOps);
  }

  public static final class PutOp extends Op {

    private final ByteString value;
    private final PutOption option;

    protected PutOp(ByteString key, ByteString value, PutOption option) {
      super(Type.PUT, key);
      this.value = value;
      this.option = option;
    }

    RequestOp toRequestOp(ByteSequence namespace) {
      PutRequest put = PutRequest.newBuilder()
          .setKey(Util.prefixNamespace(this.key, namespace))
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

    RequestOp toRequestOp(ByteSequence namespace) {
      RangeRequest.Builder range = RangeRequest.newBuilder()
          .setKey(Util.prefixNamespace(this.key, namespace))
          .setCountOnly(this.option.isCountOnly())
          .setLimit(this.option.getLimit())
          .setRevision(this.option.getRevision())
          .setKeysOnly(this.option.isKeysOnly())
          .setSerializable(this.option.isSerializable())
          .setSortOrder(toRangeRequestSortOrder(this.option.getSortOrder()))
          .setSortTarget(toRangeRequestSortTarget(this.option.getSortField()));

      this.option.getEndKey()
          .map(endKey -> Util.prefixNamespaceToRangeEnd(this.key,
              ByteString.copyFrom(endKey.getBytes()), namespace))
          .ifPresent(range::setRangeEnd);

      return RequestOp.newBuilder().setRequestRange(range).build();
    }
  }

  public static final class DeleteOp extends Op {

    private final DeleteOption option;

    protected DeleteOp(ByteString key, DeleteOption option) {
      super(Type.DELETE_RANGE, key);
      this.option = option;
    }

    RequestOp toRequestOp(ByteSequence namespace) {
      DeleteRangeRequest.Builder delete = DeleteRangeRequest.newBuilder()
          .setKey(Util.prefixNamespace(this.key, namespace))
          .setPrevKv(this.option.isPrevKV());

      this.option.getEndKey()
          .map(endKey -> Util.prefixNamespaceToRangeEnd(this.key,
              ByteString.copyFrom(endKey.getBytes()), namespace))
          .ifPresent(delete::setRangeEnd);

      return RequestOp.newBuilder().setRequestDeleteRange(delete).build();
    }
  }

  public static final class TxnOp extends Op {

    private Cmp[] cmps;

    private Op[] thenOps;

    private Op[] elseOps;

    protected TxnOp(Cmp[] cmps, Op[] thenOps, Op[] elseOps) {
      super(Type.TXN, null);
      this.cmps = cmps;
      this.thenOps = thenOps;
      this.elseOps = elseOps;
    }

    RequestOp toRequestOp(ByteSequence namespace) {
      TxnRequest.Builder txn = TxnRequest.newBuilder();

      if (cmps != null) {
        for (Cmp cmp : cmps) {
          txn.addCompare(cmp.toCompare(namespace));
        }
      }

      if (thenOps != null) {
        for (Op thenOp : thenOps) {
          txn.addSuccess(thenOp.toRequestOp(namespace));
        }
      }

      if (elseOps != null) {
        for (Op elseOp : elseOps) {
          txn.addFailure(elseOp.toRequestOp(namespace));
        }
      }

      return RequestOp.newBuilder().setRequestTxn(txn).build();
    }
  }
}
