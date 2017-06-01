package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.api.SnapshotResponse;

public class SnapshotReaderResponseWithError {

  public SnapshotResponse snapshotResponse;
  public Exception error;

  public SnapshotReaderResponseWithError(SnapshotResponse snapshotResponse) {
    this.snapshotResponse = snapshotResponse;
  }

  public SnapshotReaderResponseWithError(Exception e) {
    this.error = e;
  }
}
