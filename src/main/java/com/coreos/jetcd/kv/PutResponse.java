package com.coreos.jetcd.kv;

import com.coreos.jetcd.data.Header;
import com.coreos.jetcd.data.KeyValue;

public class PutResponse {

  private Header header;

  private KeyValue prevKv;

  private boolean hasPrevKv;

  public PutResponse(Header header, KeyValue keyValue, boolean hasPrevKv) {
    this.header = header;
    this.prevKv = keyValue;
    this.hasPrevKv = hasPrevKv;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * return previous key-value pair.
   */
  public KeyValue getPrevKv() {
    return prevKv;
  }

  public boolean hasPrevKv() {
    return this.hasPrevKv;
  }
}
