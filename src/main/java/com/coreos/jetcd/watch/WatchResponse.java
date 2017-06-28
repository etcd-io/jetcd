package com.coreos.jetcd.watch;

import com.coreos.jetcd.data.Header;
import java.util.List;

public class WatchResponse {

  private Header header;
  private List<WatchEvent> events;

  public WatchResponse(Header header, List<WatchEvent> events) {
    this.header = header;
    this.events = events;
  }

  public Header getHeader() {
    return header;
  }

  public List<WatchEvent> getEvents() {
    return events;
  }
}
