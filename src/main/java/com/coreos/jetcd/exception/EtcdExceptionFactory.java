package com.coreos.jetcd.exception;

/**
 * A factory for creating instances of {@link EtcdException} and its subtypes.
 */
public final class EtcdExceptionFactory {

  public static EtcdException newEtcdException(String message, Throwable cause) {
    return new EtcdException(message, cause);
  }

  public static EtcdException newEtcdException(Throwable cause) {
    return newEtcdException(null, cause);
  }

  public static EtcdException newEtcdException(String message) {
    return newEtcdException(message, null);
  }

  public static CompactedException newCompactedException(long compactedRev) {
    return new CompactedException(compactedRev, "required revision has been compacted", null);
  }

  public static EtcdException handleInterrupt(InterruptedException e) {
    Thread.currentThread().interrupt();
    return newEtcdException("Interrupted", e);
  }

  public static AuthFailedException newAuthFailedException(String cause, Throwable throwable) {
    return new AuthFailedException(cause, throwable);
  }

  public static ConnectException newConnectException(String cause, Throwable throwable) {
    return new ConnectException(cause, throwable);
  }
}
