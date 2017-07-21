package com.coreos.jetcd.data;

import com.coreos.jetcd.api.ResponseHeader;

public class AbstractResponse<R> implements Response {

  private final R response;
  private final ResponseHeader responseHeader;
  private final Header header;

  public AbstractResponse(R response, ResponseHeader responseHeader) {
    this.response = response;
    this.responseHeader = responseHeader;

    this.header = new HeaderImpl();
  }

  @Override
  public Header getHeader() {
    return header;
  }

  @Override
  public String toString() {
    return response.toString();
  }

  protected final R getResponse() {
    return this.response;
  }

  protected final ResponseHeader getResponseHeader() {
    return this.responseHeader;
  }

  private class HeaderImpl implements Response.Header {

    @Override
    public long getClusterId() {
      return responseHeader.getClusterId();
    }

    @Override
    public long getMemberId() {
      return responseHeader.getMemberId();
    }

    @Override
    public long getRevision() {
      return responseHeader.getRevision();
    }

    @Override
    public long getRaftTerm() {
      return responseHeader.getRaftTerm();
    }
  }
}
