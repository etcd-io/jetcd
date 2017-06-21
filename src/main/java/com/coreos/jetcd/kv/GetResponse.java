package com.coreos.jetcd.kv;

import com.coreos.jetcd.data.Header;
import com.coreos.jetcd.data.KeyValue;
import java.util.List;

public class GetResponse {

  private Header header;
  private List<KeyValue> kvs;
  private boolean more;
  private long count;

  public GetResponse(Header header, List<KeyValue> kvs, boolean more, long count) {
    this.header = header;
    this.kvs = kvs;
    this.more = more;
    this.count = count;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * return a list of key-value pairs matched by the range request.
   */
  public List<KeyValue> getKvs() {
    return kvs;
  }

  /**
   * more indicates if there are more keys to return in the requested range.
   */
  public boolean isMore() {
    return more;
  }

  /**
   * return the number of keys within the range when requested.
   */
  public long getCount() {
    return count;
  }
}
