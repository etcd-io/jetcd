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

package com.coreos.jetcd;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.ClosedClientException;
import com.coreos.jetcd.internal.impl.CloseableClient;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchResponse;

/**
 * Interface of the watch client.
 */
public interface Watch extends CloseableClient {

  /**
   * watch on a key with option.
   *
   * @param key key to be watched on.
   * @param watchOption see {@link com.coreos.jetcd.options.WatchOption}.
   * @throws ClosedClientException if watch client has been closed.
   */
  Watcher watch(ByteSequence key, WatchOption watchOption);


  /**
   * watch on a key.
   *
   * @param key key to be watched on.
   * @throws ClosedClientException if watch client has been closed.
   **/
  Watcher watch(ByteSequence key);

  /**
   * Interface of Watcher.
   */
  interface Watcher extends AutoCloseable {

    /**
     * closes this watcher and all its resources.
     **/
    @Override
    void close();

    /**
     * Retrieves next watch key, waiting if there are none.
     *
     * @throws com.coreos.jetcd.exception.ClosedWatcherException if watcher has been closed.
     * @throws ClosedClientException if watch client has been closed.
     * @throws com.coreos.jetcd.exception.CompactedException when watch a key at a revision that has
     *        been compacted.
     * @throws com.coreos.jetcd.exception.EtcdException when listen encounters connection error,
     *        etcd server error, or any internal client error.
     * @throws InterruptedException when listen thread is interrupted.
     */
    WatchResponse listen() throws InterruptedException;
  }
}
