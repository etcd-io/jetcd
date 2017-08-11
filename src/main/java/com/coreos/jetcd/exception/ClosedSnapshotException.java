package com.coreos.jetcd.exception;

public class ClosedSnapshotException extends EtcdException {

  public ClosedSnapshotException() {
    super(ErrorCode.CANCELLED, "Snapshot has been closed", null);
  }
}
