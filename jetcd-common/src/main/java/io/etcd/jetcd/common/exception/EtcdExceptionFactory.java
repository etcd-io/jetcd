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

package io.etcd.jetcd.common.exception;

import java.util.Objects;

import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcStatus;

/**
 * A factory for creating instances of {@link EtcdException} and its subtypes.
 */
public final class EtcdExceptionFactory {

    /**
     * Private constructor to prevent instantiation.
     */
    private EtcdExceptionFactory() {
    }

    /**
     * Creates a new EtcdException with the specified error code and message.
     *
     * @param  code    the error code
     * @param  message the error message
     * @return         a new EtcdException
     */
    public static EtcdException newEtcdException(ErrorCode code, String message) {
        return new EtcdException(code, message, null);
    }

    /**
     * Creates a new EtcdException with the specified error code, message, and cause.
     *
     * @param  code    the error code
     * @param  message the error message
     * @param  cause   the cause of the exception
     * @return         a new EtcdException
     */
    public static EtcdException newEtcdException(ErrorCode code, String message, Throwable cause) {
        return new EtcdException(code, message, cause);
    }

    /**
     * Creates a CompactedException when a requested revision has been compacted.
     *
     * @param  compactedRev the latest compacted revision
     * @return              a new CompactedException
     */
    public static CompactedException newCompactedException(long compactedRev) {
        return new CompactedException(ErrorCode.OUT_OF_RANGE, "etcdserver: mvcc: required revision has been compacted",
            compactedRev);
    }

    /**
     * Creates a ClosedWatcherException when operations are attempted on a closed watcher.
     *
     * @return a new ClosedWatcherException
     */
    public static ClosedWatcherException newClosedWatcherException() {
        return new ClosedWatcherException();
    }

    /**
     * Creates a ClosedClientException for a closed watch client.
     *
     * @return a new ClosedClientException
     */
    public static ClosedClientException newClosedWatchClientException() {
        return new ClosedClientException("Watch Client has been closed");
    }

    /**
     * Creates a ClosedClientException for a closed lease client.
     *
     * @return a new ClosedClientException
     */
    public static ClosedClientException newClosedLeaseClientException() {
        return new ClosedClientException("Lease Client has been closed");
    }

    /**
     * Creates a ClosedKeepAliveListenerException when operations are attempted on a closed listener.
     *
     * @return a new ClosedKeepAliveListenerException
     */
    public static ClosedKeepAliveListenerException newClosedKeepAliveListenerException() {
        return new ClosedKeepAliveListenerException();
    }

    /**
     * Creates a ClosedSnapshotException when operations are attempted on a closed snapshot.
     *
     * @return a new ClosedSnapshotException
     */
    public static ClosedSnapshotException newClosedSnapshotException() {
        return new ClosedSnapshotException();
    }

    /**
     * Handles InterruptedException by restoring the interrupt status and creating an EtcdException.
     * This ensures proper interrupt handling while converting to the exception model.
     *
     * @param  e the InterruptedException
     * @return   an EtcdException with CANCELLED error code
     */
    public static EtcdException handleInterrupt(InterruptedException e) {
        Thread.currentThread().interrupt();
        return newEtcdException(ErrorCode.CANCELLED, "Interrupted", e);
    }

    /**
     * Converts a Throwable to an EtcdException.
     * If the throwable is already an EtcdException, returns it unchanged.
     * If it's an InvalidStatusException, extracts the gRPC status.
     * Otherwise, wraps it in a new EtcdException with UNKNOWN error code.
     *
     * @param  cause the throwable to convert
     * @return       an EtcdException
     */
    public static EtcdException toEtcdException(Throwable cause) {
        Objects.requireNonNull(cause, "cause can't be null");
        if (cause instanceof EtcdException etcdException) {
            return etcdException;
        }

        if (cause instanceof InvalidStatusException statusEx) {
            return toEtcdException(statusEx.actualStatus());
        }

        return newEtcdException(ErrorCode.UNKNOWN, cause.getMessage(), cause);
    }

    /**
     * Converts a gRPC status to an EtcdException.
     *
     * @param  status the gRPC status
     * @return        an EtcdException with the corresponding error code
     */
    public static EtcdException toEtcdException(GrpcStatus status) {
        return toEtcdException(status, null, null);
    }

    /**
     * Converts a gRPC failure while retaining the server's message and the underlying cause.
     *
     * @param  status  the gRPC status
     * @param  message the server's status message, or null to use the status value
     * @param  cause   the underlying cause
     * @return         an EtcdException with the corresponding error code
     */
    public static EtcdException toEtcdException(GrpcStatus status, String message, Throwable cause) {
        Objects.requireNonNull(status, "status can't be null");
        return newEtcdException(ErrorCode.fromGrpcStatus(status), Objects.requireNonNullElse(message, status.toString()),
            cause);
    }
}
