package io.etcd.jetcd.support;

import java.util.Optional;
import java.util.function.Consumer;

import io.etcd.jetcd.ByteSequence;
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

/**
 * Utility class for mapping client requests to gRPC requests.
 */
public final class Requests {

    private Requests() {
    }

    /**
     * Maps a get operation to a range request.
     *
     * @param  key       the key
     * @param  option    the get option
     * @param  namespace the namespace
     * @return           the range request
     */
    public static RangeRequest mapRangeRequest(ByteSequence key, GetOption option, ByteSequence namespace) {
        RangeRequest.Builder builder = RangeRequest.newBuilder()
            .setKey(Util.prefixNamespace(key, namespace))
            .setCountOnly(option.countOnly())
            .setLimit(option.limit())
            .setRevision(option.revision())
            .setKeysOnly(option.keysOnly())
            .setSerializable(option.serializable())
            .setSortOrder(toRangeRequestSortOrder(option.sortOrder()))
            .setSortTarget(toRangeRequestSortTarget(option.getSortField()))
            .setMinCreateRevision(option.minCreateRevision())
            .setMaxCreateRevision(option.maxCreateRevision())
            .setMinModRevision(option.minModRevision())
            .setMaxModRevision(option.maxModRevision());

        defineRangeRequestEnd(key, option.getEndKey(), option.prefix(), namespace, builder::setRangeEnd);
        return builder.build();
    }

    /**
     * Maps a put operation to a put request.
     *
     * @param  key       the key
     * @param  value     the value
     * @param  option    the put option
     * @param  namespace the namespace
     * @return           the put request
     */
    public static PutRequest mapPutRequest(ByteSequence key, ByteSequence value, PutOption option, ByteSequence namespace) {
        return PutRequest.newBuilder()
            .setKey(Util.prefixNamespace(key, namespace))
            .setValue(ByteString.copyFrom(value.getBytes()))
            .setLease(option.leaseId())
            .setPrevKv(option.prevKV())
            .setIgnoreValue(option.ignoreValue())
            .setIgnoreLease(option.ignoreLease())
            .build();
    }

    /**
     * Maps a delete operation to a delete range request.
     *
     * @param  key       the key
     * @param  option    the delete option
     * @param  namespace the namespace
     * @return           the delete range request
     */
    public static DeleteRangeRequest mapDeleteRequest(ByteSequence key, DeleteOption option, ByteSequence namespace) {
        DeleteRangeRequest.Builder builder = DeleteRangeRequest.newBuilder()
            .setKey(Util.prefixNamespace(key, namespace))
            .setPrevKv(option.prevKV());

        defineRangeRequestEnd(key, option.getEndKey(), option.prefix(), namespace, builder::setRangeEnd);

        return builder.build();
    }

    private static void defineRangeRequestEnd(
        ByteSequence key,
        Optional<ByteSequence> endKeyOptional,
        boolean hasPrefix,
        ByteSequence namespace,
        Consumer<ByteString> setRangeEndConsumer) {

        if (endKeyOptional.isPresent()) {
            setRangeEndConsumer
                .accept(Util.prefixNamespaceToRangeEnd(ByteString.copyFrom(endKeyOptional.get().getBytes()), namespace));
        } else {
            if (hasPrefix) {
                ByteSequence endKey = OptionsUtil.prefixEndOf(key);
                setRangeEndConsumer.accept(Util.prefixNamespaceToRangeEnd(ByteString.copyFrom(endKey.getBytes()), namespace));
            }
        }
    }
}
