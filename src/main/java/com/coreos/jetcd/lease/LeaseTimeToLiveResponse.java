package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.ByteSequence;
import java.util.List;
import java.util.stream.Collectors;

public class LeaseTimeToLiveResponse extends
    AbstractResponse<com.coreos.jetcd.api.LeaseTimeToLiveResponse> {

  private List<ByteSequence> keys;

  public LeaseTimeToLiveResponse(com.coreos.jetcd.api.LeaseTimeToLiveResponse response) {
    super(response, response.getHeader());
  }

  /**
   * ID is the lease ID from the keep alive request.
   */
  public long getID() {
    return getResponse().getID();
  }

  /**
   * TTL is the remaining TTL in seconds for the lease;
   * the lease will expire in under TTL+1 seconds.
   */
  public long getTTl() {
    return getResponse().getTTL();
  }

  /**
   * GrantedTTL is the initial granted time in seconds upon lease creation/renewal.
   */
  public long getGrantedTTL() {
    return getResponse().getGrantedTTL();
  }

  /**
   * Keys is the list of keys attached to this lease.
   */
  public synchronized List<ByteSequence> getKeys() {
    if (keys == null) {
      keys = getResponse().getKeysList().stream()
          .map(byteStrings -> ByteSequence.fromBytes(byteStrings.toByteArray()))
          .collect(Collectors.toList());
    }

    return keys;
  }
}
