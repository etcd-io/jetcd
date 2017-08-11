package com.coreos.jetcd.exception;

public class ClosedKeepAliveListenerException extends EtcdException {

  public ClosedKeepAliveListenerException() {
    super(ErrorCode.CANCELLED, "KeepAliveListener has been closed", null);
  }
}
