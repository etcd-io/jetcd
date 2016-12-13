package com.coreos.jetcd;

import com.google.common.util.concurrent.ListenableFuture;

import com.coreos.jetcd.api.LeaseGrantResponse;
import com.coreos.jetcd.api.ResponseHeader;
import com.coreos.jetcd.data.EtcdHeader;
import com.coreos.jetcd.lease.Lease;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * This util is to convert api class to client class.
 */
class EtcdUtil {

    private EtcdUtil() {
    }

    /**
     * convert API response header to self defined header
     */
    protected static EtcdHeader apiToClientHeader(ResponseHeader header) {
        return new EtcdHeader(header.getClusterId(), header.getMemberId(), header.getRevision(), header.getRaftTerm());
    }

    protected static Lease apiToClientLease(LeaseGrantResponse response) {
        return new Lease(response.getID(), response.getTTL(), apiToClientHeader(response.getHeader()));
    }

    static <S, T> CompletableFuture<T> completableFromListenableFuture(final ListenableFuture<S> sourceFuture, final FutureResultConvert<S, T> resultConvert, Executor executor) {
        CompletableFuture<T> targetFuture = new CompletableFuture<T>() {
            /**
             * If not already completed, completes this CompletableFuture with
             * a {@link CancellationException}. Dependent CompletableFutures
             * that have not already completed will also complete
             * exceptionally, with a {@link CompletionException} caused by
             * this {@code CancellationException}.
             *
             * @param mayInterruptIfRunning this value has no effect in this
             *                              implementation because interrupts are not used to control
             *                              processing.
             * @return {@code true} if this task is now cancelled
             */
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = sourceFuture.cancel(mayInterruptIfRunning);
                super.cancel(mayInterruptIfRunning);
                return result;
            }
        };
        sourceFuture.addListener(() -> {
            try {
                targetFuture.complete(resultConvert.convert(sourceFuture.get()));
            } catch (Exception e) {
                targetFuture.completeExceptionally(e);
            }
        }, executor);
        return targetFuture;
    }


    interface FutureResultConvert<S, T> {
        T convert(S source);
    }
}
