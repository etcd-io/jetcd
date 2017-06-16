package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Header;
import java.util.List;

public class LeaseTimeToLiveResponse {

  private Header header;
  private long leaseId;
  private long ttl;
  private long grantedTtl;
  private List<ByteSequence> keys;

  public LeaseTimeToLiveResponse(Header header, long leaseId, long ttl, long grantedTtl,
      List<ByteSequence> keys) {
    this.header = header;
    this.leaseId = leaseId;
    this.ttl = ttl;
    this.grantedTtl = grantedTtl;
    this.keys = keys;
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
   * TTL is the remaining TTL in seconds for the lease;
   * the lease will expire in under TTL+1 seconds.
   */
  public long getTTl() {
    return ttl;
  }

  /**
   * GrantedTTL is the initial granted time in seconds upon lease creation/renewal.
   */
  public long getGrantedTTL() {
    return grantedTtl;
  }

  /**
   * Keys is the list of keys attached to this lease.
   */
  public List<ByteSequence> getKeys() {
    return keys;
  }

}
