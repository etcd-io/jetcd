/*
 * Copyright 2016-2019 The jetcd authors
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

import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Interface of the watch client.
 */
public interface Watch extends CloseableClient {

  /**
   * watch on a key with option.
   *
   * @param key key to be watched on.
   * @param option see {@link io.etcd.jetcd.options.WatchOption}.
   * @throws ClosedClientException if watch client has been closed.
   */
  Watcher watch(ByteSequence key, WatchOption option, Listener listener);

  /**
   * watch on a key.
   *
   * @param key key to be watched on.
   * @throws ClosedClientException if watch client has been closed.
   **/
  default Watcher watch(
      ByteSequence key,
      Listener listener) {
    return watch(key, WatchOption.DEFAULT, listener);
  }

  default Watcher watch(
      ByteSequence key,
      Consumer<WatchResponse> onNext) {
    return watch(key, WatchOption.DEFAULT, listener(onNext));
  }

  default Watcher watch(
      ByteSequence key,
      Consumer<WatchResponse> onNext,
      Consumer<Throwable> onError) {
    return watch(key, WatchOption.DEFAULT, listener(onNext, onError));
  }

  default Watcher watch(
      ByteSequence key,
      Consumer<WatchResponse> onNext,
      Consumer<Throwable> onError,
      Runnable onCompleted) {
    return watch(key, WatchOption.DEFAULT, listener(onNext, onError, onCompleted));
  }

  default Watcher watch(
      ByteSequence key,
      Consumer<WatchResponse> onNext,
      Runnable onCompleted) {
    return watch(key, WatchOption.DEFAULT, listener(onNext, t -> { }, onCompleted));
  }

  default Watcher watch(
      ByteSequence key,
      WatchOption option,
      Consumer<WatchResponse> onNext) {
    return watch(key, option, listener(onNext));
  }

  default Watcher watch(
      ByteSequence key,
      WatchOption option,
      Consumer<WatchResponse> onNext,
      Consumer<Throwable> onError) {
    return watch(key, option, listener(onNext, onError));
  }

  default Watcher watch(
      ByteSequence key,
      WatchOption option,
      Consumer<WatchResponse> onNext,
      Runnable onCompleted) {
    return watch(key, option, listener(onNext, t -> { }, onCompleted));
  }

  default Watcher watch(
      ByteSequence key,
      WatchOption option,
      Consumer<WatchResponse> onNext,
      Consumer<Throwable> onError,
      Runnable onCompleted) {
    return watch(key, option, listener(onNext, onError, onCompleted));
  }

  static Listener listener(Consumer<WatchResponse> onNext) {
    return listener(onNext, t -> { }, () -> { });
  }

  static Listener listener(Consumer<WatchResponse> onNext, Consumer<Throwable> onError) {
    return listener(onNext, onError, () -> { });
  }

  static Listener listener(Consumer<WatchResponse> onNext, Runnable onCompleted) {
    return listener(onNext, t -> { }, onCompleted);
  }

  static Listener listener(Consumer<WatchResponse> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
    return new Listener() {
      @Override
      public void onNext(WatchResponse response) {
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
   * Interface of Watcher.
   */
  interface Listener {
    /**
     * Invoked on new events.
     *
     * @param response the response.
     */
    void onNext(WatchResponse response);

    /**
     * Invoked on errors.
     *
     * @param throwable the error.
     */
    void onError(Throwable throwable);

    /**
     * Invoked on completion.
     */
    void onCompleted();
  }

  interface Watcher extends Closeable {
    /**
     * closes this watcher and all its resources.
     */
    @Override
    void close();
  }
}
