package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.data.Header;

/**
 * DefragmentResponse returned by {@link Maintenance#defragmentMember(String)} contains a header.
 */
public class DefragmentResponse {

  private final Header header;

  public DefragmentResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
