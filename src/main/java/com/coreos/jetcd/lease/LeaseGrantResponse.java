package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.Header;

public class LeaseGrantResponse {

  private Header header;
  private long leaseId;
  private long ttl;

  public LeaseGrantResponse(Header header, long leaseId, long ttl) {
    this.header = header;
    this.leaseId = leaseId;
    this.ttl = ttl;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * ID is the lease ID for the granted lease.
   */
  public long getID() {
    return leaseId;
  }

  /**
   * TTL is the server chosen lease time-to-live in seconds.
   */
  public long getTTL() {
    return ttl;
  }
}
