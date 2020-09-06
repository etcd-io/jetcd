/*
 * Copyright 2016-2020 The jetcd authors
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

package io.etcd.jetcd.options;

import java.util.Optional;

import io.etcd.jetcd.ByteSequence;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The option for watch operation.
 */
public final class WatchOption {

    public static final WatchOption DEFAULT = newBuilder().build();

    /**
     * Create a builder to construct option for watch operation.
     *
     * @return builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private long revision = 0L;
        private Optional<ByteSequence> endKey = Optional.empty();
        private boolean prevKV = false;
        private boolean progressNotify = false;
        private boolean noPut = false;
        private boolean noDelete = false;
        private boolean requireLeader = false;

        private Builder() {
        }

        /**
         * Provide the revision to use for the get request.
         *
         * <p>
         * If the revision is less or equal to zero, the get is over the newest key-value store.
         *
         * <p>
         * If the revision has been compacted, ErrCompacted is returned as a response.
         *
         * @param  revision the revision to get.
         * @return          builder
         */
        public Builder withRevision(long revision) {
            this.revision = revision;
            return this;
        }

        /**
         * Set the end key of the get request. If it is set, the get request will return the keys from
         * <i>key</i> to <i>endKey</i> (exclusive).
         *
         * <p>
         * If end key is '\0', the range is all keys {@literal >=} key.
         *
         * <p>
         * If the end key is one bit larger than the given key, then it gets all keys with the prefix
         * (the given key).
         *
         * <p>
         * If both key and end key are '\0', it returns all keys.
         *
         * @param  endKey end key
         * @return        builder
         */
        public Builder withRange(ByteSequence endKey) {
            this.endKey = Optional.ofNullable(endKey);
            return this;
        }

        /**
         * When prevKV is set, created watcher gets the previous KV before the event happens,
         * if the previous KV is not compacted.
         *
         * @param  prevKV configure the watcher to receive previous KV.
         * @return        builder
         */
        public Builder withPrevKV(boolean prevKV) {
            this.prevKV = prevKV;
            return this;
        }

        /**
         * When progressNotify is set, the watch server send periodic progress updates.
         * Progress updates have zero events in WatchResponse.
         *
         * @param  progressNotify configure the watcher to receive progress updates.
         * @return                builder
         */
        public Builder withProgressNotify(boolean progressNotify) {
            this.progressNotify = progressNotify;
            return this;
        }

        /**
         * filter out put event in server side.
         *
         * @param  noPut filter out put event
         * @return       builder
         */
        public Builder withNoPut(boolean noPut) {
            this.noPut = noPut;
            return this;
        }

        /**
         * filter out delete event in server side.
         *
         * @param  noDelete filter out delete event
         * @return          builder
         */
        public Builder withNoDelete(boolean noDelete) {
            this.noDelete = noDelete;
            return this;
        }

        /**
         * Enables watch all the keys with matching prefix.
         *
         * @param  prefix the common prefix of all the keys that you want to watch
         * @return        builder
         */
        public Builder withPrefix(ByteSequence prefix) {
            checkNotNull(prefix, "prefix should not be null");
            ByteSequence prefixEnd = OptionsUtil.prefixEndOf(prefix);
            this.withRange(prefixEnd);
            return this;
        }

        /**
         * When creating the watch streaming stub, use the REQUIRED_LEADER Metadata annotation,
         * which ensures the stream will error out if quorum is lost by
         * the server the stream is connected to.
         * Without this option, a stream running against a server that is out of quorum
         * simply goes silent.
         *
         * @param  requireLeader require quorum for watch stream creation.
         * @return               builder
         */
        public Builder withRequireLeader(boolean requireLeader) {
            this.requireLeader = requireLeader;
            return this;
        }

        public WatchOption build() {
            return new WatchOption(endKey, revision, prevKV, progressNotify, noPut, noDelete, requireLeader);
        }

    }

    private final Optional<ByteSequence> endKey;
    private final long revision;
    private final boolean prevKV;
    private final boolean progressNotify;
    private final boolean noPut;
    private final boolean noDelete;
    private final boolean requireLeader;

    private WatchOption(Optional<ByteSequence> endKey, long revision, boolean prevKV, boolean progressNotify, boolean noPut,
        boolean noDelete, boolean requireLeader) {
        this.endKey = endKey;
        this.revision = revision;
        this.prevKV = prevKV;
        this.progressNotify = progressNotify;
        this.noPut = noPut;
        this.noDelete = noDelete;
        this.requireLeader = requireLeader;
    }

    public Optional<ByteSequence> getEndKey() {
        return this.endKey;
    }

    /**
     * @return the revision to watch from.
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Whether created watcher gets the previous KV before the event happens.
     *
     * @return if true, watcher receives the previous KV before the event happens.
     */
    public boolean isPrevKV() {
        return prevKV;
    }

    /**
     * Whether watcher server send periodic progress updates.
     *
     * @return if true, watcher server should send periodic progress updates.
     */
    public boolean isProgressNotify() {
        return progressNotify;
    }

    /**
     * Whether filter put event in server side.
     *
     * @return if true, filter put event in server side
     */
    public boolean isNoPut() {
        return noPut;
    }

    /**
     * Whether filter delete event in server side.
     *
     * @return if true, filter delete event in server side
     */
    public boolean isNoDelete() {
        return noDelete;
    }

    /**
     * If true, when creating the watch streaming stub, use the REQUIRED_LEADER Metadata annotation,
     * which ensures the stream will error out if quorum is lost by
     * the server the stream is connected to. This will make the watch fail with an error
     * and finish.
     * Without this option, a watch running against a server that is out of quorum
     * simply goes silent.
     *
     * @return if true, use REQUIRE_LEADER metadata annotation for watch streams
     */
    public boolean withRequireLeader() {
        return requireLeader;
    }
}
