package com.coreos.jetcd.exception;

/**
 * CompactedException is thrown when a operation wants to retrieve key at a revision that has
 * been compacted.
 */
public class CompactedException extends EtcdException {

  private long compactedRevision;

  CompactedException(ErrorCode code, String message, long compactedRev) {
    super(code, message, null);
    this.compactedRevision = compactedRev;
  }

  // get the current compacted revision of etcd server.
  public long getCompactedRevision() {
    return compactedRevision;
  }
}
