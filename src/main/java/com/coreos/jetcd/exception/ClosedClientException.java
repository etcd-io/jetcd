package com.coreos.jetcd.exception;

public class ClosedClientException extends EtcdException {

  public ClosedClientException(String reason) {
    super(ErrorCode.CANCELLED, reason, null);
  }
}
