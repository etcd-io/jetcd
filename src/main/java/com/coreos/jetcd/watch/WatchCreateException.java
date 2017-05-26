package com.coreos.jetcd.watch;

import com.coreos.jetcd.data.Header;

/**
 * Exception thrown when create watcher failed.
 */
public class WatchCreateException extends Exception {

  public final Header header;

  public WatchCreateException(String cause, Header header) {
    super(cause);
    this.header = header;
  }
}
