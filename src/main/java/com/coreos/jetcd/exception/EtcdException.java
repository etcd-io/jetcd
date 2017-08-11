package com.coreos.jetcd.exception;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base exception type for all exceptions produced by the etcd service.
 */
public class EtcdException extends RuntimeException {

  private final ErrorCode code;

  EtcdException(ErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = checkNotNull(code);
  }

  /**
   * Returns the error code associated with this exception.
   */
  public ErrorCode getErrorCode() {
    return code;
  }
}
