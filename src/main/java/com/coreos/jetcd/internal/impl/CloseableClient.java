package com.coreos.jetcd.internal.impl;

public interface CloseableClient {
  /**
   * close the client and release its resources.
   */
  default void close() {
    // noop
  }

}
