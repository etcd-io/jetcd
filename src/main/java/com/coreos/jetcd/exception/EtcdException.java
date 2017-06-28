package com.coreos.jetcd.exception;

/**
 * Base exception type for all exceptions produced by the etcd service.
 */
public class EtcdException extends RuntimeException {
  EtcdException(String message, Throwable cause) {
    super(message, cause);
  }
}
