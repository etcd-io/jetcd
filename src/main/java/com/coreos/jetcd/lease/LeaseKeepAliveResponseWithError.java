package com.coreos.jetcd.lease;

import com.coreos.jetcd.api.LeaseKeepAliveResponse;

/**
 * Created by fanminshi on 6/8/17.
 */
public class LeaseKeepAliveResponseWithError {

  public LeaseKeepAliveResponse leaseKeepAliveResponse;
  public Exception error;

  public LeaseKeepAliveResponseWithError(LeaseKeepAliveResponse leaseKeepAliveResponse) {
    this.leaseKeepAliveResponse = leaseKeepAliveResponse;
  }

  public LeaseKeepAliveResponseWithError(Exception e) {
    this.error = e;
  }
}
