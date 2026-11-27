/*
 * Copyright 2016-2021 The jetcd authors
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

import java.time.Duration;
import java.util.Optional;

import io.etcd.jetcd.ByteSequence;

import static java.util.Objects.requireNonNull;

/**
 * The option for watch operation.
 *
 * @param endKey                end key for range watch
 * @param revision              revision to watch from
 * @param prevKV                whether to include previous key-value
 * @param progressNotify        whether to receive progress notifications
 * @param createdNotify         whether to receive creation notification
 * @param noPut                 whether to filter put events
 * @param noDelete              whether to filter delete events
 * @param requireLeader         whether to require leader
 * @param prefix                whether to watch by prefix
 * @param fragment              whether to enable splitting large revisions into multiple watch responses
 * @param maxReconnectAttempts  maximum number of reconnection attempts
 * @param initialReconnectDelay initial delay before reconnection
 * @param maxReconnectDelay     maximum delay between reconnection attempts
 * @param reconnectJitter       maximum random variance added to retry delays to prevent
 *                              synchronized retry storms. Set to Duration.ZERO to disable.
 */
public record WatchOption(
    ByteSequence endKey,
    long revision,
    boolean prevKV,
    boolean progressNotify,
    boolean createdNotify,
    boolean noPut,
    boolean noDelete,
    boolean requireLeader,
    boolean prefix,
    boolean fragment,
    int maxReconnectAttempts,
    Duration initialReconnectDelay,
    Duration maxReconnectDelay,
    Duration reconnectJitter) {

    public static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 10;
    public static final Duration DEFAULT_INITIAL_RECONNECT_DELAY = Duration.ofMillis(500);
    public static final Duration DEFAULT_MAX_RECONNECT_DELAY = Duration.ofSeconds(30);
    public static final Duration DEFAULT_RECONNECT_JITTER = Duration.ofMillis(250);

    public static final WatchOption DEFAULT = builder().build();

    /**
     * Returns the end key for range watches.
     *
     * @return the end key, or empty if not set
     */
    public Optional<ByteSequence> getEndKey() {
        return Optional.ofNullable(this.endKey);
    }

    /**
     * Returns the revision to watch from.
     *
     * @return the revision.
     */
    @Override
    public long revision() {
        return revision;
    }

    /**
     * Whether created watcher gets the previous KV before the event happens.
     *
     * @return if true, watcher receives the previous KV before the event happens.
     */
    @Override
    public boolean prevKV() {
        return prevKV;
    }

    /**
     * Whether watcher server send periodic progress updates.
     *
     * @return if true, watcher server should send periodic progress updates.
     */
    @Override
    public boolean progressNotify() {
        return progressNotify;
    }

    /**
     * Whether watcher server send watch create event.
     *
     * @return if true, watcher server should send watch create event.
     */
    @Override
    public boolean createdNotify() {
        return createdNotify;
    }

    /**
     * Whether filter put event in server side.
     *
     * @return if true, filter put event in server side
     */
    @Override
    public boolean noPut() {
        return noPut;
    }

    /**
     * Whether filter delete event in server side.
     *
     * @return if true, filter delete event in server side
     */
    @Override
    public boolean noDelete() {
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

    /**
     * Returns the maximum number of reconnection attempts.
     *
     * @return the maximum number of reconnection attempts
     */
    @Override
    public int maxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    /**
     * Returns the initial delay before the first reconnection attempt.
     *
     * @return the initial reconnection delay
     */
    @Override
    public Duration initialReconnectDelay() {
        return initialReconnectDelay;
    }

    /**
     * Returns the maximum delay between reconnection attempts.
     *
     * @return the maximum reconnection delay
     */
    @Override
    public Duration maxReconnectDelay() {
        return maxReconnectDelay;
    }

    /**
     * Whether fragment is enabled for splitting large revisions into multiple watch responses.
     *
     * @return if true, enable fragmenting large watch responses
     */
    @Override
    public boolean fragment() {
        return fragment;
    }

    /**
     * Returns the maximum random variance added to retry delays.
     * Jitter prevents synchronized retry storms when multiple clients fail simultaneously.
     *
     * @return the reconnection jitter duration
     */
    @Override
    public Duration reconnectJitter() {
        return reconnectJitter;
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for WatchOption.
     */
    public static final class Builder {
        private long revision = 0L;
        private ByteSequence endKey;
        private boolean prevKV = false;
        private boolean progressNotify = false;
        private boolean createNotify = false;
        private boolean noPut = false;
        private boolean noDelete = false;
        private boolean requireLeader = false;
        private boolean prefix = false;
        private boolean fragment = false;
        private int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
        private Duration initialReconnectDelay = DEFAULT_INITIAL_RECONNECT_DELAY;
        private Duration maxReconnectDelay = DEFAULT_MAX_RECONNECT_DELAY;
        private Duration reconnectJitter = DEFAULT_RECONNECT_JITTER;

        private Builder() {
        }

        /**
         * Provide the revision to use for the watch request.
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
         * Set the end key of the watch request. If it is set, the get request will return the keys from
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
            this.endKey = endKey;
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
         * When createNotify is set, the watch server sends event when watch is created.
         *
         * @param  createNotify configure the watcher to receive watch create event.
         * @return              builder
         */
        public Builder withCreateNotify(boolean createNotify) {
            this.createNotify = createNotify;
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
         * Enables watch watch all the keys by prefix.
         *
         * @param  prefix flag to watch all the keys by prefix
         * @return        builder
         */
        public Builder isPrefix(boolean prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Enables watch all the keys with matching prefix.
         *
         * @param      prefix the common prefix of all the keys that you want to watch
         * @return            builder
         * @deprecated        Use {@link #isPrefix(boolean)} instead.
         */
        @Deprecated
        public Builder withPrefix(ByteSequence prefix) {
            requireNonNull(prefix, "prefix should not be null");
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

        /**
         * Sets the maximum number of reconnection attempts before giving up.
         *
         * @param  maxReconnectAttempts the maximum number of attempts (default: 10)
         * @return                      builder
         */
        public Builder withMaxReconnectAttempts(int maxReconnectAttempts) {
            this.maxReconnectAttempts = maxReconnectAttempts;
            return this;
        }

        /**
         * Sets the initial delay before the first reconnection attempt.
         * Subsequent attempts use exponential backoff up to maxReconnectDelay.
         *
         * @param  initialReconnectDelay the initial delay (default: 500ms)
         * @return                       builder
         */
        public Builder withInitialReconnectDelay(Duration initialReconnectDelay) {
            this.initialReconnectDelay = requireNonNull(initialReconnectDelay);
            return this;
        }

        /**
         * Sets the maximum delay between reconnection attempts.
         *
         * @param  maxReconnectDelay the maximum delay (default: 30s)
         * @return                   builder
         */
        public Builder withMaxReconnectDelay(Duration maxReconnectDelay) {
            this.maxReconnectDelay = requireNonNull(maxReconnectDelay);
            return this;
        }

        /**
         * Lets etcd split large watch responses into smaller messages on the wire.
         * Jetcd assembles the fragments before delivering the response to the listener.
         *
         * @param  fragment whether to enable fragmenting
         * @return          builder
         */
        public Builder withFragment(boolean fragment) {
            this.fragment = fragment;
            return this;
        }

        /**
         * Sets the maximum random variance added to retry delays.
         * Jitter prevents synchronized retry storms when multiple clients fail simultaneously.
         *
         * @param  jitter maximum random time to add to retry delays (default: 100ms)
         * @return        builder
         */
        public Builder withReconnectJitter(Duration jitter) {
            this.reconnectJitter = requireNonNull(jitter);
            return this;
        }

        /**
         * Builds the WatchOption.
         *
         * @return the watch option
         */
        public WatchOption build() {
            return new WatchOption(
                endKey,
                revision,
                prevKV,
                progressNotify,
                createNotify,
                noPut,
                noDelete,
                requireLeader,
                prefix,
                fragment,
                maxReconnectAttempts,
                initialReconnectDelay,
                maxReconnectDelay,
                reconnectJitter);
        }

    }
}
