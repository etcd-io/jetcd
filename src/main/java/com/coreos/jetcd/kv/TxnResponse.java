package com.coreos.jetcd.kv;

import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * TxnResponse returned by a transaction call contains lists of put, get, delete responses
 * corresponding to either the compare in txn.IF is evaluated to true or false.
 */
public class TxnResponse {

  private final Header header;
  private final boolean succeeded;
  private final List<PutResponse> putResponses;
  private final List<GetResponse> getResponses;
  private final List<DeleteResponse> deleteResponses;
  // TODO add txnResponses when nested txn is implemented.

  public TxnResponse(Header header, boolean succeeded, List<PutResponse> putResponses,
      List<GetResponse> getResponses,
      List<DeleteResponse> deleteResponses) {
    this.header = header;
    this.succeeded = succeeded;
    this.deleteResponses = deleteResponses;
    this.getResponses = getResponses;
    this.putResponses = putResponses;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * return true if the compare evaluated to true or false otherwise.
   */
  public boolean isSucceeded() {
    return succeeded;
  }

  /**
   * returns a list of DeleteResponse; empty list if none.
   */
  public List<DeleteResponse> getDeleteResponses() {
    return deleteResponses;
  }

  /**
   * returns a list of GetResponse; empty list if none.
   */
  public List<GetResponse> getGetResponses() {
    return getResponses;
  }

  /**
   * returns a list of PutResponse; empty list if none.
   */
  public List<PutResponse> getPutResponses() {
    return putResponses;
  }
}
