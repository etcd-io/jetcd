package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.Header;

public class LeaseKeepAliveResponse {

  private Header header;
  private long leaseId;
  private long ttl;

  public LeaseKeepAliveResponse(Header header, long leaseId, long ttl) {
    this.header = header;
    this.leaseId = leaseId;
    this.ttl = ttl;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * ID is the lease ID from the keep alive request.
   */
  public long getID() {
    return leaseId;
  }

  /**
   * TTL is the new time-to-live for the lease.
   */
  public long getTTL() {
    return ttl;
  }
}
