package com.coreos.jetcd.lease;

import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.exception.EtcdException;

/**
 * Created by fanminshi on 6/8/17.
 */
public class LeaseKeepAliveResponseWithError {

  public LeaseKeepAliveResponse leaseKeepAliveResponse;
  public EtcdException error;

  public LeaseKeepAliveResponseWithError(LeaseKeepAliveResponse leaseKeepAliveResponse) {
    this.leaseKeepAliveResponse = leaseKeepAliveResponse;
  }

  public LeaseKeepAliveResponseWithError(EtcdException e) {
    this.error = e;
  }
}
