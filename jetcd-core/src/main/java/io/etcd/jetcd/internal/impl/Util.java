/**
 * Copyright 2017 The jetcd authors
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

package io.etcd.jetcd.internal.impl;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.handleInterrupt;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.newEtcdException;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

import com.google.common.util.concurrent.ListenableFuture;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.grpc.Status;
import io.grpc.Status.Code;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * a util class for jetcd.
 */
final class Util {

  private Util() {
  }

  /**
   * convert ListenableFuture of Type S to CompletableFuture of Type T.
   */
  static <S, T> CompletableFuture<T> toCompletableFuture(
      ListenableFuture<S> sourceFuture, Function<S, T> resultConvert, Executor executor) {
    CompletableFuture<T> targetFuture = new CompletableFuture<T>() {
      // the cancel of targetFuture also cancels the sourceFuture.
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        return sourceFuture.cancel(mayInterruptIfRunning);
      }
    };

    sourceFuture.addListener(() -> {
      try {
        targetFuture.complete(resultConvert.apply(sourceFuture.get()));
      } catch (Exception e) {
        targetFuture.completeExceptionally(toEtcdException(e));
      }
    }, executor);

    return targetFuture;
  }

  /**
   * converts a ListenableFuture of Type S to a CompletableFuture of Type T with retry on
   * ListenableFuture error.
   *
   * @param newSourceFuture a function that returns a new SourceFuture.
   * @param resultConvert a function that converts Type S to Type T.
   * @param executor a executor.
   * @param <S> Source type
   * @param <T> Converted Type.
   * @return a CompletableFuture with type T.
   */
  static <S, T> CompletableFuture<T> toCompletableFutureWithRetry(
      Supplier<ListenableFuture<S>> newSourceFuture,
      Function<S, T> resultConvert,
      Executor executor) {
    return toCompletableFutureWithRetry(newSourceFuture, resultConvert, Util::isRetriable, executor);
  }

  /**
   * converts a ListenableFuture of Type S to a CompletableFuture of Type T with retry on
   * ListenableFuture error.
   *
   * @param newSourceFuture a function that returns a new SourceFuture.
   * @param resultConvert a function that converts Type S to Type T.
   * @param doRetry a function that determines the retry condition base on SourceFuture error.
   * @param executor a executor.
   * @param <S> Source type
   * @param <T> Converted Type.
   * @return a CompletableFuture with type T.
   */
  static <S, T> CompletableFuture<T> toCompletableFutureWithRetry(
      Supplier<ListenableFuture<S>> newSourceFuture,
      Function<S, T> resultConvert,
      Function<Exception, Boolean> doRetry,
      Executor executor) {

    AtomicReference<ListenableFuture<S>> sourceFutureRef = new AtomicReference<>();
    sourceFutureRef.lazySet(newSourceFuture.get());

    CompletableFuture<T> targetFuture = new CompletableFuture<T>() {
      // the cancel of targetFuture also cancels the sourceFuture.
      @Override
      public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);
        ListenableFuture<S> sourceFuture = sourceFutureRef.get();
        if (sourceFuture != null) {
          cancelled = sourceFuture.cancel(true);
        }
        return cancelled;
      }
    };

    executor.execute(() -> {
      // only retry 3 times.
      int retryLimit = 3;
      while (retryLimit-- > 0) {
        try {
          ListenableFuture<S> f = sourceFutureRef.get();
          targetFuture.complete(resultConvert.apply(f.get()));
          return;
        } catch (Exception e) {
          if (doRetry.apply(e)) {
            synchronized (targetFuture) {
              if (targetFuture.isCancelled()) {
                // don't retry if targetFuture has cancelled.
                return;
              }
              sourceFutureRef.set(newSourceFuture.get());
            }
            try {
              Thread.sleep(500);
            } catch (InterruptedException e1) {
              // raise interrupted exception to caller.
              targetFuture.completeExceptionally(handleInterrupt(e1));
              return;
            }
            continue;
          }
          targetFuture.completeExceptionally(e);
          return;
        }
      }
      // notify user that retry has failed.
      targetFuture.completeExceptionally(
          newEtcdException(
              ErrorCode.ABORTED,
              "maximum number of auto retries reached"
          )
      );
    });

    return targetFuture;
  }

  static boolean isRetriable(Exception e) {
    return isInvalidTokenError(Status.fromThrowable(e));
  }

  static boolean isInvalidTokenError(Status status) {
    return status.getCode() == Code.UNAUTHENTICATED
        && "etcdserver: invalid auth token".equals(status.getDescription());
  }

  static <T> void applyIfNotNull(T target, Consumer<T> consumer) {
    if (target != null) {
      consumer.accept(target);
    }
  }

  static <T> T supplyIfNull(T target, Supplier<T> supplier) {
    return target != null ? target : supplier.get();
  }
}
