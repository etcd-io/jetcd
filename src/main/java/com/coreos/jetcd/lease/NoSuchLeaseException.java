package com.coreos.jetcd.lease;

/**
 * Signals that the lease client do not have this lease
 */
public class NoSuchLeaseException extends Exception {

  public NoSuchLeaseException(long leaseId) {
    super("No such lease: " + leaseId);
  }
}
