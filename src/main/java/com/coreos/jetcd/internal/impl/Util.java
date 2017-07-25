package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.data.ByteSequence;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * a util class for jetcd.
 */
final class Util {

  private static final Logger logger = Logger.getLogger(Util.class.getName());

  // RETRIABLE_ERRORS are etcd errors that can be retried.
  private static final List<String> RETRIABLE_ERRORS = Stream.of("etcdserver: invalid auth token")
      .collect(Collectors.toList());

  private Util() {
  }

  /**
   * convert ByteSequence to ByteString.
   */
  static ByteString byteStringFromByteSequence(ByteSequence byteSequence) {
    return ByteString.copyFrom(byteSequence.getBytes());
  }

  /**
   * convert ByteString to ByteSequence.
   */
  static ByteSequence byteSequenceFromByteString(ByteString byteString) {
    return ByteSequence.fromBytes(byteString.toByteArray());
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
        targetFuture.completeExceptionally(e);
      }
    }, executor);

    return targetFuture;
  }

  /**
   * converts a ListenableFuture of Type S to a CompletableFuture of Type T with retry on
   * ListenableFuture error.
   *
   * @param sourceFuture the source future.
   * @param resultConvert a function that converts Type S to Type T.
   * @param newSourceFuture a function that returns a new SourceFuture.
   * @param doRetry a function that determines the retry condition base on SourceFuture error.
   * @param executor a executor.
   * @param <S> Source type
   * @param <T> Converted Type.
   * @return a CompletableFuture with type T.
   */
  static <S, T> CompletableFuture<T> toCompletableFutureWithRetry(
      ListenableFuture<S> sourceFuture,
      Function<S, T> resultConvert,
      Supplier<ListenableFuture<S>> newSourceFuture,
      Function<Exception, Boolean> doRetry,
      Executor executor) {

    AtomicReference<ListenableFuture<S>> sourceFutureRef = new AtomicReference<>();
    sourceFutureRef.lazySet(sourceFuture);

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
      while (true) {
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
            continue;
          }
          targetFuture.completeExceptionally(e);
          return;
        }
      }
    });

    return targetFuture;
  }

  static boolean retry(Exception e) {
    if (e == null || e.getCause() == null || e.getCause().getMessage() == null) {
      return false;
    }

    return RETRIABLE_ERRORS.stream().anyMatch(e.getCause().getMessage()::contains);
  }
}
