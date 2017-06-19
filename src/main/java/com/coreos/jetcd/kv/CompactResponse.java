package com.coreos.jetcd.kv;

import com.coreos.jetcd.data.Header;

public class CompactResponse {

  private Header header;

  public CompactResponse(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }
}
