package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.Header;

public class LeaseRevokeResponse {

  private Header header;

  public LeaseRevokeResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
