package com.coreos.jetcd.kv;

import com.coreos.jetcd.data.Header;
import com.coreos.jetcd.data.KeyValue;
import java.util.List;

public class DeleteResponse {

  private Header header;
  private long deleted;
  private List<KeyValue> prevKvs;

  public DeleteResponse(Header header, long deleted, List<KeyValue> prevKvs) {
    this.header = header;
    this.deleted = deleted;
    this.prevKvs = prevKvs;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * return the number of keys deleted by the delete range request.
   */
  public long getDeleted() {
    return deleted;
  }

  /**
   * return previous key-value pairs.
   */
  public List<KeyValue> getPrevKvs() {
    return prevKvs;
  }
}
