package com.coreos.jetcd.watch;

import com.coreos.jetcd.api.WatchResponse;
import com.coreos.jetcd.exception.EtcdException;

public class WatchResponseWithError {

  private WatchResponse watchResponse;
  private EtcdException exception;

  private WatchResponseWithError() {

  }

  public WatchResponseWithError(WatchResponse watchResponse) {
    this.watchResponse = watchResponse;
  }

  public WatchResponseWithError(EtcdException e) {
    this.exception = e;
  }


  public WatchResponse getWatchResponse() {
    return watchResponse;
  }

  public EtcdException getException() {
    return exception;
  }
}
