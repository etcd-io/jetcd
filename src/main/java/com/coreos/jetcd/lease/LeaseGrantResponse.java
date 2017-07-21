package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.AbstractResponse;

public class LeaseGrantResponse extends AbstractResponse<com.coreos.jetcd.api.LeaseGrantResponse> {

  public LeaseGrantResponse(com.coreos.jetcd.api.LeaseGrantResponse response) {
    super(response, response.getHeader());
  }

  /**
   * ID is the lease ID for the granted lease.
   */
  public long getID() {
    return getResponse().getID();
  }

  /**
   * TTL is the server chosen lease time-to-live in seconds.
   */
  public long getTTL() {
    return getResponse().getTTL();
  }
}
