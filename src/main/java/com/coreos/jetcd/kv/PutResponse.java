package com.coreos.jetcd.kv;

import com.coreos.jetcd.data.AbstractResponse;
import com.coreos.jetcd.data.KeyValue;

public class PutResponse extends AbstractResponse<com.coreos.jetcd.api.PutResponse> {

  public PutResponse(com.coreos.jetcd.api.PutResponse putResponse) {
    super(putResponse, putResponse.getHeader());
  }

  /**
   * return previous key-value pair.
   */
  public KeyValue getPrevKv() {
    return new KeyValue(getResponse().getPrevKv());
  }

  public boolean hasPrevKv() {
    return getResponse().hasPrevKv();
  }
}
