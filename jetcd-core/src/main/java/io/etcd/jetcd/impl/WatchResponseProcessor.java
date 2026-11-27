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

import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.Errors;

import com.google.common.base.Strings;

import static io.etcd.jetcd.common.exception.ErrorCode.FAILED_PRECONDITION;
import static io.etcd.jetcd.common.exception.ErrorCode.INTERNAL;
import static io.etcd.jetcd.common.exception.ErrorCode.OUT_OF_RANGE;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newCompactedException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;

/**
 * Processes etcd WatchResponse messages and classifies them into result types.
 * This class contains pure logic for response classification, separate from
 * the stateful WatchConnection that handles the actual actions.
 */
final class WatchResponseProcessor {

    /**
     * Result of processing a WatchResponse.
     */
    sealed interface Result {

        /**
         * Authentication error - requires token refresh and reconnect.
         */
        record AuthError() implements Result {
        }

        /**
         * Watch was created successfully.
         */
        record Created(long revision, boolean shouldNotify) implements Result {
        }

        /**
         * Watch was canceled - includes error details.
         */
        record Canceled(Throwable error) implements Result {
        }

        /**
         * Progress notification.
         */
        record Progress(long revision, boolean withNamespace) implements Result {
        }

        /**
         * Events received.
         */
        record Events(long newRevision) implements Result {
        }

        /**
         * Response was not recognized or should be ignored.
         */
        record Ignored() implements Result {
        }
    }

    private final boolean createdNotify;
    private final boolean progressNotify;

    WatchResponseProcessor(WatchOption option) {
        this.createdNotify = option.createdNotify();
        this.progressNotify = option.progressNotify();
    }

    /**
     * Process a WatchResponse and return the classification result.
     *
     * @param  response the response to process
     * @return          the processing result
     */
    Result process(WatchResponse response) {
        Result authResult = checkAuthError(response);
        if (authResult != null) {
            return authResult;
        }

        Result createdResult = checkCreated(response);
        if (createdResult != null) {
            return createdResult;
        }

        Result canceledResult = checkCanceled(response);
        if (canceledResult != null) {
            return canceledResult;
        }

        Result progressResult = checkProgress(response);
        if (progressResult != null) {
            return progressResult;
        }

        Result eventsResult = checkEvents(response);
        if (eventsResult != null) {
            return eventsResult;
        }

        return new Result.Ignored();
    }

    private Result checkAuthError(WatchResponse response) {
        if (!response.getCreated() || !response.getCanceled()) {
            return null;
        }

        String cancelReason = response.getCancelReason();
        if (cancelReason.isEmpty()) {
            return null;
        }

        if (Errors.isAuthenticationError(cancelReason)) {
            return new Result.AuthError();
        }

        return null;
    }

    private Result checkCreated(WatchResponse response) {
        if (!response.getCreated()) {
            return null;
        }

        if (response.getWatchId() == -1) {
            return new Result.Canceled(
                newEtcdException(INTERNAL, "etcd server failed to create watch id"));
        }

        return new Result.Created(
            response.getHeader().getRevision(),
            createdNotify);
    }

    private Result checkCanceled(WatchResponse response) {
        if (!response.getCanceled()) {
            return null;
        }

        Throwable error;
        String reason = response.getCancelReason();

        if (response.getCompactRevision() != 0) {
            error = newCompactedException(response.getCompactRevision());
        } else if (Strings.isNullOrEmpty(reason)) {
            error = newEtcdException(OUT_OF_RANGE,
                "etcdserver: mvcc: required revision is a future revision");
        } else {
            error = newEtcdException(FAILED_PRECONDITION, reason);
        }

        return new Result.Canceled(error);
    }

    private Result checkProgress(WatchResponse response) {
        if (io.etcd.jetcd.watch.WatchResponse.isProgressNotify(response)) {
            return new Result.Progress(response.getHeader().getRevision(), false);
        }

        if (response.getEventsCount() == 0 && progressNotify) {
            return new Result.Progress(response.getHeader().getRevision(), true);
        }

        return null;
    }

    private Result checkEvents(WatchResponse response) {
        if (response.getEventsCount() == 0) {
            return null;
        }

        // Calculate new revision from last event
        long newRevision = response.getEvents(response.getEventsCount() - 1)
            .getKv()
            .getModRevision() + 1;

        return new Result.Events(newRevision);
    }
}
