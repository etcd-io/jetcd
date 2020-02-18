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

package io.etcd.jetcd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.etcd.jetcd.api.WatchCancelRequest;
import io.etcd.jetcd.api.WatchCreateRequest;
import io.etcd.jetcd.api.WatchGrpc;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.options.WatchOption;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newClosedWatchClientException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newCompactedException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

/**
 * watch Implementation.
 */
final class WatchImpl implements Watch {
    private static final Logger LOG = LoggerFactory.getLogger(WatchImpl.class);

    private final Object lock;
    private final ClientConnectionManager connectionManager;
    private final WatchGrpc.WatchStub stub;
    private final ListeningScheduledExecutorService executor;
    private final AtomicBoolean closed;
    private final List<WatcherImpl> watchers;
    private final ByteSequence namespace;

    WatchImpl(ClientConnectionManager connectionManager) {
        this.lock = new Object();
        this.connectionManager = connectionManager;
        this.stub = connectionManager.newStub(WatchGrpc::newStub);
        this.executor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
        this.closed = new AtomicBoolean();
        this.watchers = new ArrayList<>();
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

    private final class WatcherImpl implements Watcher, StreamObserver<WatchResponse> {
        private final ByteSequence key;
        private final WatchOption option;
        private final Listener listener;
        private final AtomicBoolean closed;

        private StreamObserver<WatchRequest> stream;
        private long revision;
        private long id;

        WatcherImpl(ByteSequence key, WatchOption option, Listener listener) {
            this.key = key;
            this.option = option;
            this.listener = listener;
            this.closed = new AtomicBoolean();

            this.stream = null;
            this.id = -1;
            this.revision = this.option.getRevision();
        }

        // ************************
        //
        // Lifecycle
        //
        // ************************

        void resume() {
            if (this.closed.get() || WatchImpl.this.closed.get()) {
                return;
            }

            if (stream == null) {
                // id is not really useful today but it may be in etcd 3.4
                id = -1;

                WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
                    .setKey(Util.prefixNamespace(this.key.getByteString(), namespace)).setPrevKv(this.option.isPrevKV())
                    .setProgressNotify(option.isProgressNotify()).setStartRevision(this.revision);

                option.getEndKey().map(endKey -> Util.prefixNamespaceToRangeEnd(endKey.getByteString(), namespace))
                    .ifPresent(builder::setRangeEnd);

                if (option.isNoDelete()) {
                    builder.addFilters(WatchCreateRequest.FilterType.NODELETE);
                }

                if (option.isNoPut()) {
                    builder.addFilters(WatchCreateRequest.FilterType.NOPUT);
                }

                stream = stub.watch(this);
                stream.onNext(WatchRequest.newBuilder().setCreateRequest(builder).build());
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                if (stream != null) {
                    WatchCancelRequest watchCancelRequest = WatchCancelRequest.newBuilder().setWatchId(this.id).build();

                    if (id != -1) {
                        stream.onNext(WatchRequest.newBuilder().setCancelRequest(watchCancelRequest).build());
                    }

                    if (stream != null) {
                        stream.onCompleted();
                        stream = null;
                    }
                }

                id = -1;

                listener.onCompleted();
            }
        }

        // ************************
        //
        // StreamObserver
        //
        // ************************

        @Override
        public void onNext(WatchResponse response) {
            if (closed.get()) {
                // events eventually received when the client is closed should
                // not be propagated to the listener
                return;
            }

            if (response.getCreated()) {

                //
                // Created
                //

                if (response.getWatchId() == -1) {
                    listener.onError(newEtcdException(ErrorCode.INTERNAL, "etcd server failed to create watch id"));
                    return;
                }

                revision = response.getHeader().getRevision();
                id = response.getWatchId();
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

                listener.onError(error);
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

        @Override
        public void onError(Throwable t) {
            if (WatcherImpl.this.closed.get() || WatchImpl.this.closed.get()) {
                return;
            }

            Status status = Status.fromThrowable(t);
            listener.onError(toEtcdException(status));

            stream.onCompleted();
            stream = null;

            Futures.addCallback(executor.schedule(this::resume, 500, TimeUnit.MILLISECONDS), new FutureCallback<Object>() {
                @Override
                public void onFailure(Throwable throwable) {
                    LOG.error("scheduled resume failed", throwable);
                }

                @Override
                public void onSuccess(Object result) {
                }
            }, executor);
        }

        @Override
        public void onCompleted() {
        }
    }
}
