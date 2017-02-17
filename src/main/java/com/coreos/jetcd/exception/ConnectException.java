package com.coreos.jetcd.exception;

/**
 * Signals that an error occurred while attempting to connect to
 * etcd.
 */
public class ConnectException extends Exception {

  public ConnectException(String reason, Throwable cause) {
    super(reason, cause);
  }

  public ConnectException() {
  }
}
