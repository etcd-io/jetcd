package com.coreos.jetcd.exception;

public class ClosedWatcherException extends EtcdException {

  public ClosedWatcherException() {
    super(ErrorCode.CANCELLED, "Watcher has been closed", null);
  }
}
