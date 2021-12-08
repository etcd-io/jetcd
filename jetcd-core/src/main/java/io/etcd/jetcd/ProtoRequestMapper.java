package io.etcd.jetcd;

import java.util.Optional;
import java.util.function.Consumer;

import io.etcd.jetcd.api.DeleteRangeRequest;
import io.etcd.jetcd.api.PutRequest;
import io.etcd.jetcd.api.RangeRequest;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.OptionsUtil;
import io.etcd.jetcd.options.PutOption;

import com.google.protobuf.ByteString;

import static io.etcd.jetcd.options.OptionsUtil.toRangeRequestSortOrder;
import static io.etcd.jetcd.options.OptionsUtil.toRangeRequestSortTarget;

public final class ProtoRequestMapper {

    private ProtoRequestMapper() {
    }

    public static RangeRequest mapRangeRequest(ByteSequence key, GetOption option, ByteSequence namespace) {
        RangeRequest.Builder builder = RangeRequest.newBuilder()
            .setKey(Util.prefixNamespace(key.getByteString(), namespace))
            .setCountOnly(option.isCountOnly())
            .setLimit(option.getLimit())
            .setRevision(option.getRevision())
            .setKeysOnly(option.isKeysOnly())
            .setSerializable(option.isSerializable())
            .setSortOrder(toRangeRequestSortOrder(option.getSortOrder()))
            .setSortTarget(toRangeRequestSortTarget(option.getSortField()))
            .setMinCreateRevision(option.getMinCreateRevision())
            .setMaxCreateRevision(option.getMaxCreateRevision())
            .setMinModRevision(option.getMinModRevision())
            .setMaxModRevision(option.getMaxModRevision());

        defineRangeRequestEnd(key, option.getEndKey(), option.isPrefix(), namespace, builder::setRangeEnd);
        return builder.build();
    }

    public static PutRequest mapPutRequest(ByteSequence key, ByteSequence value, PutOption option, ByteSequence namespace) {
        return PutRequest.newBuilder()
            .setKey(Util.prefixNamespace(key.getByteString(), namespace))
            .setValue(value.getByteString())
            .setLease(option.getLeaseId())
            .setPrevKv(option.getPrevKV())
            .build();
    }

    public static DeleteRangeRequest mapDeleteRequest(ByteSequence key, DeleteOption option, ByteSequence namespace) {
        DeleteRangeRequest.Builder builder = DeleteRangeRequest.newBuilder()
            .setKey(Util.prefixNamespace(key.getByteString(), namespace))
            .setPrevKv(option.isPrevKV());
        defineRangeRequestEnd(key, option.getEndKey(), option.isPrefix(), namespace, builder::setRangeEnd);
        return builder.build();
    }

    private static void defineRangeRequestEnd(ByteSequence key, Optional<ByteSequence> endKeyOptional,
        boolean hasPrefix, ByteSequence namespace, Consumer<ByteString> setRangeEndConsumer) {
        endKeyOptional.ifPresentOrElse(endKey -> {
            setRangeEndConsumer.accept(Util.prefixNamespaceToRangeEnd(ByteString.copyFrom(endKey.getBytes()), namespace));
        }, () -> {
            if (hasPrefix) {
                ByteSequence endKey = OptionsUtil.prefixEndOf(key);
                setRangeEndConsumer.accept(Util.prefixNamespaceToRangeEnd(ByteString.copyFrom(endKey.getBytes()), namespace));
            }
        });
    }
}
