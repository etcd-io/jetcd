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

  public long getLeaseId() {
    return leaseId;
  }

  public long getTTl() {
    return ttl;
  }

  public long getGrantedTTL() {
    return grantedTtl;
  }

  public List<ByteSequence> getKeys() {
    return keys;
  }

}
