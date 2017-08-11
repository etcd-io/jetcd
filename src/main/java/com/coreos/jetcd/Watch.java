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
  interface Watcher {

    /**
     * closes this watcher and all its resources.
     **/
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
