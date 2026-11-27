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

package io.etcd.jetcd;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.support.CloseableClient;

/**
 * Interface of KeepAlive talking to etcd.
 */
public interface Lease extends CloseableClient {

    /**
     * Grants a lease with the given TTL.
     * The operation is retried only after a confirmed token rejection.
     *
     * @param  ttl ttl value, unit seconds
     * @return     the grant response
     */
    CompletableFuture<LeaseGrantResponse> grant(long ttl);

    /**
     * Grants a lease with the given TTL and a deadline covering authentication and any auth retry.
     * If the deadline expires, the future fails with an
     * {@link EtcdException} with code {@link ErrorCode#DEADLINE_EXCEEDED}.
     *
     * @param  ttl     ttl value, unit seconds
     * @param  timeout the maximum time to wait. A nonpositive value creates an expired deadline
     * @param  unit    the time unit of the timeout argument
     * @return         the grant response
     */
    CompletableFuture<LeaseGrantResponse> grant(long ttl, long timeout, TimeUnit unit);

    /**
     * revoke one lease and the key bind to this lease will be removed.
     *
     * @param  leaseId id of the lease to revoke
     * @return         the revoke response
     */
    CompletableFuture<LeaseRevokeResponse> revoke(long leaseId);

    /**
     * keep alive one lease only once.
     *
     * @param  leaseId id of lease to keep alive once
     * @return         The keep alive response
     */
    CompletableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId);

    /**
     * retrieves the lease information of the given lease ID.
     *
     * @param  leaseId     id of lease
     * @param  leaseOption LeaseOption
     * @return             LeaseTimeToLiveResponse wrapped in CompletableFuture
     */
    CompletableFuture<LeaseTimeToLiveResponse> timeToLive(long leaseId, LeaseOption leaseOption);

    /**
     * Keeps the given lease alive and delivers callbacks serially for this listener registration on worker threads.
     * Each keepalive response or reconnect error replaces any pending notification of the same kind.
     * Terminal notifications are delivered after the callbacks still pending for the registration.
     * The client continues renewing the lease while the listener is busy.
     *
     * @param  leaseId  lease to be keep alive forever.
     * @param  listener the listener
     * @return          a CloseableClient that can be used to stop the keep alive.
     */
    CloseableClient keepAlive(long leaseId, Listener listener);

    /**
     * keep the given lease alive forever.
     *
     * @param  leaseId lease to be keep alive forever.
     * @param  onNext  the on next consumer
     * @return         a CloseableClient that can be used to stop the keep alive.
     */
    default CloseableClient keepAlive(long leaseId, Consumer<LeaseKeepAliveResponse> onNext) {
        return keepAlive(leaseId, listener(onNext));
    }

    /**
     * keep the given lease alive forever.
     *
     * @param  leaseId lease to be keep alive forever.
     * @param  onNext  the on next consumer
     * @param  onError the on error consumer
     * @return         a CloseableClient that can be used to stop the keep alive.
     */
    default CloseableClient keepAlive(long leaseId, Consumer<LeaseKeepAliveResponse> onNext, Consumer<Throwable> onError) {
        return keepAlive(leaseId, listener(onNext, onError));
    }

    /**
     * keep the given lease alive forever.
     *
     * @param  leaseId     lease to be keep alive forever.
     * @param  onNext      the on next consumer
     * @param  onError     the on error consumer
     * @param  onCompleted the on completion runnable
     * @return             a CloseableClient that can be used to stop the keep alive.
     */
    default CloseableClient keepAlive(long leaseId, Consumer<LeaseKeepAliveResponse> onNext, Consumer<Throwable> onError,
        Runnable onCompleted) {
        return keepAlive(leaseId, listener(onNext, onError, onCompleted));
    }

    /**
     * Creates a listener with only onNext callback.
     *
     * @param  onNext the callback for responses
     * @return        the listener
     */
    static Listener listener(Consumer<LeaseKeepAliveResponse> onNext) {
        return listener(onNext, t -> {
        }, () -> {
        });
    }

    /**
     * Creates a listener with onNext and onError callbacks.
     *
     * @param  onNext  the callback for responses
     * @param  onError the callback for errors
     * @return         the listener
     */
    static Listener listener(Consumer<LeaseKeepAliveResponse> onNext, Consumer<Throwable> onError) {
        return listener(onNext, onError, () -> {
        });
    }

    /**
     * Creates a listener with all callbacks.
     *
     * @param  onNext      the callback for responses
     * @param  onError     the callback for errors
     * @param  onCompleted the callback when completed
     * @return             the listener
     */
    static Listener listener(Consumer<LeaseKeepAliveResponse> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        return new Listener() {
            @Override
            public void onNext(LeaseKeepAliveResponse response) {
                onNext.accept(response);
            }

            @Override
            public void onError(Throwable throwable) {
                onError.accept(throwable);
            }

            @Override
            public void onCompleted() {
                onCompleted.run();
            }
        };
    }

    /**
     * Listener for lease keepalive responses. See {@link Lease#keepAlive(long, Listener)} for callback delivery.
     */
    interface Listener {
        /**
         * Invoked on new keepalive responses. A busy listener may skip intermediate responses.
         *
         * @param response the response.
         */
        void onNext(LeaseKeepAliveResponse response);

        /**
         * Invoked on errors. A busy listener may skip intermediate reconnect errors.
         *
         * @param throwable the error.
         */
        void onError(Throwable throwable);

        /**
         * Invoked on completion.
         */
        void onCompleted();
    }
}
