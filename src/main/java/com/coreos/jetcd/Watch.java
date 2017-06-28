package com.coreos.jetcd;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchResponse;

/**
 * Interface of the watch client.
 */
public interface Watch {

  /**
   * watch on a key with option.
   *
   * @param key key to be watched on.
   * @param watchOption see {@link com.coreos.jetcd.options.WatchOption}.
   */
  Watcher watch(ByteSequence key, WatchOption watchOption);


  /**
   * watch on a key.
   *
   * @param key key to be watched on.
   **/
  Watcher watch(ByteSequence key);

  /**
   * releases all watch client resources.
   * note, this won't close active watchers.
   */
  void close();

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
     * @throws com.coreos.jetcd.exception.EtcdException when listen encounters connection error,
     *        watcher closed, watch client closed, etcd server error, and any internal client error.
     * @throws com.coreos.jetcd.exception.CompactedException when watch a key at a revision that has
     *        been compacted.
     */
    WatchResponse listen();
  }
}
