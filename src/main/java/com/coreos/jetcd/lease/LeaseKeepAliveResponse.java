package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.AbstractResponse;

public class LeaseKeepAliveResponse extends
    AbstractResponse<com.coreos.jetcd.api.LeaseKeepAliveResponse> {

  public LeaseKeepAliveResponse(com.coreos.jetcd.api.LeaseKeepAliveResponse response) {
    super(response, response.getHeader());
  }

  /**
   * ID is the lease ID from the keep alive request.
   */
  public long getID() {
    return getResponse().getID();
  }

  /**
   * TTL is the new time-to-live for the lease.
   */
  public long getTTL() {
    return getResponse().getTTL();
  }
}
