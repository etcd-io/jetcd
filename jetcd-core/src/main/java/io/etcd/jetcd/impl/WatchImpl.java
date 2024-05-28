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

package io.etcd.jetcd.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.api.VertxWatchGrpc;
import io.etcd.jetcd.api.WatchCancelRequest;
import io.etcd.jetcd.api.WatchCreateRequest;
import io.etcd.jetcd.api.WatchProgressRequest;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.options.OptionsUtil;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.support.Util;
import io.grpc.Status;
import io.vertx.core.streams.WriteStream;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedWatchClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newCompactedException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

/**
 * watch Implementation.
 */
final class WatchImpl extends Impl implements Watch {
    private static final Logger LOG = LoggerFactory.getLogger(WatchImpl.class);

    private final Object lock;
    private final VertxWatchGrpc.WatchVertxStub stub;
    private final ListeningScheduledExecutorService executor;
    private final AtomicBoolean closed;
    private final List<WatcherImpl> watchers;
    private final ByteSequence namespace;

    WatchImpl(ClientConnectionManager connectionManager) {
        super(connectionManager);

        this.lock = new Object();
        this.stub = connectionManager.newStub(VertxWatchGrpc::newVertxStub);
        // set it to daemon as there is no way for users to create this thread pool by their own
        this.executor = MoreExecutors.listeningDecorator(
            Executors.newScheduledThreadPool(1, Util.createThreadFactory("jetcd-watch-", true)));
        this.closed = new AtomicBoolean();
        this.watchers = new CopyOnWriteArrayList<>();
        this.namespace = connectionManager.getNamespace();
    }

    @Override
    public Watcher watch(ByteSequence key, WatchOption option, Listener listener) {
        if (closed.get()) {
            throw newClosedWatchClientException();
        }

        WatcherImpl impl;

        synchronized (this.lock) {
            impl = new WatcherImpl(key, option, listener);
            impl.resume();

            watchers.add(impl);
        }

        return impl;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            synchronized (this.lock) {
                executor.shutdownNow();
                watchers.forEach(Watcher::close);
            }
        }
    }

    @Override
    public void requestProgress() {
        if (!closed.get()) {
            synchronized (this.lock) {
                watchers.forEach(Watcher::requestProgress);
            }
        }
    }

    final class WatcherImpl implements Watcher {
        private final ByteSequence key;
        private final WatchOption option;
        private final Listener listener;
        private final AtomicBoolean closed;

        private final AtomicReference<WriteStream<WatchRequest>> wstream;
        private final AtomicBoolean started;
        private long revision;
        private long id;

        WatcherImpl(ByteSequence key, WatchOption option, Listener listener) {
            this.key = key;
            this.option = option;
            this.listener = listener;
            this.closed = new AtomicBoolean();

            this.started = new AtomicBoolean();
            this.wstream = new AtomicReference<>();
            this.id = -1;
            this.revision = this.option.getRevision();
        }

        // ************************
        //
        // Lifecycle
        //
        // ************************

        @Override
        public boolean isClosed() {
            return this.closed.get() || WatchImpl.this.closed.get();
        }

        void resume() {
            if (isClosed()) {
                return;
            }

            if (started.compareAndSet(false, true)) {
                // id is not really useful today, but it may be in etcd 3.4
                id = -1;

                WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
                    .setKey(Util.prefixNamespace(this.key, namespace))
                    .setPrevKv(this.option.isPrevKV())
                    .setProgressNotify(option.isProgressNotify()).setStartRevision(this.revision);

                option.getEndKey()
                    .map(endKey -> Util.prefixNamespaceToRangeEnd(endKey, namespace))
                    .ifPresent(builder::setRangeEnd);

                if (option.getEndKey().isEmpty() && option.isPrefix()) {
                    ByteSequence endKey = OptionsUtil.prefixEndOf(key);
                    builder.setRangeEnd(Util.prefixNamespaceToRangeEnd(endKey, namespace));
                }

                if (option.isNoDelete()) {
                    builder.addFilters(WatchCreateRequest.FilterType.NODELETE);
                }

                if (option.isNoPut()) {
                    builder.addFilters(WatchCreateRequest.FilterType.NOPUT);
                }

                var ignored = Util.applyRequireLeader(option.withRequireLeader(), stub)
                    .watchWithHandler(
                        stream -> {
                            wstream.set(stream);
                            stream.write(WatchRequest.newBuilder().setCreateRequest(builder).build());
                        },
                        this::onNext,
                        event -> onCompleted(),
                        this::onError);
            }
        }

        @Override
        public void close() {

            // sync with onError()
            synchronized (WatchImpl.this.lock) {
                if (closed.compareAndSet(false, true)) {
                    if (wstream.get() != null) {
                        if (id != -1) {
                            final WatchCancelRequest watchCancelRequest = WatchCancelRequest.newBuilder().setWatchId(this.id)
                                .build();
                            final WatchRequest request = WatchRequest.newBuilder().setCancelRequest(watchCancelRequest).build();

                            wstream.get().end(request);
                        } else {
                            wstream.get().end();
                        }
                    }

                    id = -1;

                    listener.onCompleted();

                    // remote the watcher from the watchers list
                    watchers.remove(this);
                }
            }
        }

        @Override
        public void requestProgress() {
            if (!closed.get() && wstream.get() != null) {
                WatchProgressRequest watchProgressRequest = WatchProgressRequest.newBuilder().build();
                wstream.get().write(WatchRequest.newBuilder().setProgressRequest(watchProgressRequest).build());
            }
        }

        // ************************
        //
        // StreamObserver
        //
        // ************************

        private void onNext(WatchResponse response) {
            if (closed.get()) {
                // events eventually received when the client is closed should
                // not be propagated to the listener
                return;
            }

            // handle a special case when watch has been created and closed at the same time
            if (response.getCreated() && response.getCanceled() && response.getCancelReason() != null
                && (response.getCancelReason().contains("etcdserver: permission denied") ||
                    response.getCancelReason().contains("etcdserver: invalid auth token"))) {

                // potentially access token expired
                connectionManager().authCredential().refresh();
                Status error = Status.Code.CANCELLED.toStatus().withDescription(response.getCancelReason());
                handleError(toEtcdException(error), true);
            } else if (response.getCreated()) {

                //
                // Created
                //

                if (response.getWatchId() == -1) {
                    listener.onError(newEtcdException(ErrorCode.INTERNAL, "etcd server failed to create watch id"));
                    return;
                }

                revision = Math.max(revision, response.getHeader().getRevision());
                id = response.getWatchId();
                if (option.isCreatedNotify()) {
                    listener.onNext(new io.etcd.jetcd.watch.WatchResponse(response));
                }
            } else if (response.getCanceled()) {

                //
                // Cancelled
                //

                String reason = response.getCancelReason();
                Throwable error;

                if (response.getCompactRevision() != 0) {
                    error = newCompactedException(response.getCompactRevision());
                } else if (Strings.isNullOrEmpty(reason)) {
                    error = newEtcdException(ErrorCode.OUT_OF_RANGE,
                        "etcdserver: mvcc: required revision is a future revision");
                } else {
                    error = newEtcdException(ErrorCode.FAILED_PRECONDITION, reason);
                }

                handleError(toEtcdException(error), false);
            } else if (io.etcd.jetcd.watch.WatchResponse.isProgressNotify(response)) {
                listener.onNext(new io.etcd.jetcd.watch.WatchResponse(response));
                revision = Math.max(revision, response.getHeader().getRevision());
            } else if (response.getEventsCount() == 0 && option.isProgressNotify()) {

                //
                // Event
                //
                // A response may not contain events, this is in case of "Progress_Notify":
                //
                //   the watch will periodically receive a  WatchResponse with no events,
                //   if there are no recent events. It is useful when clients wish to
                //   recover a disconnected watcher starting from a recent known revision.
                //
                //   The etcd server decides how often to send notifications based on current
                //   server load.
                //
                // For more info:
                //   https://coreos.com/etcd/docs/latest/learning/api.html#watch-streams
                //
                listener.onNext(new io.etcd.jetcd.watch.WatchResponse(response, namespace));
                revision = response.getHeader().getRevision();
            } else if (response.getEventsCount() > 0) {
                listener.onNext(new io.etcd.jetcd.watch.WatchResponse(response, namespace));
                revision = response.getEvents(response.getEventsCount() - 1).getKv().getModRevision() + 1;
            }
        }

        private void onCompleted() {
            listener.onCompleted();
        }

        private void onError(Throwable t) {
            handleError(toEtcdException(t), shouldReschedule(Status.fromThrowable(t)));
        }

        private void handleError(EtcdException etcdException, boolean shouldReschedule) {
            // sync with close()
            synchronized (WatchImpl.this.lock) {
                if (isClosed()) {
                    return;
                }

                listener.onError(etcdException);
                if (wstream.get() != null) {
                    wstream.get().end();
                }
                wstream.set(null);
                started.set(false);
            }
            if (shouldReschedule) {
                if (etcdException.getMessage().contains("etcdserver: permission denied")) {
                    // potentially access token expired
                    connectionManager().authCredential().refresh();
                }

                reschedule();
                return;
            }
            close();
        }

        private boolean shouldReschedule(final Status status) {
            return !Errors.isHaltError(status) && !Errors.isNoLeaderError(status);
        }

        private void reschedule() {
            Futures.addCallback(executor.schedule(this::resume, 500, TimeUnit.MILLISECONDS), new FutureCallback<Object>() {
                @Override
                public void onFailure(@NonNull Throwable t) {
                    LOG.warn("scheduled resume failed", t);
                }

                @Override
                public void onSuccess(Object result) {
                }
            }, executor);
        }
    }
}
