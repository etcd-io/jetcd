/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coreos.jetcd.kv;

import static com.coreos.jetcd.api.ResponseOp.ResponseCase.RESPONSE_DELETE_RANGE;
import static com.coreos.jetcd.api.ResponseOp.ResponseCase.RESPONSE_PUT;
import static com.coreos.jetcd.api.ResponseOp.ResponseCase.RESPONSE_RANGE;

import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TxnResponse returned by a transaction call contains lists of put, get, delete responses
 * corresponding to either the compare in txn.IF is evaluated to true or false.
 */
public class TxnResponse extends AbstractResponse<com.coreos.jetcd.api.TxnResponse> {

  // TODO add txnResponsesRef when nested txn is implemented.
  private List<PutResponse> putResponses;
  private List<GetResponse> getResponses;
  private List<DeleteResponse> deleteResponses;


  public TxnResponse(com.coreos.jetcd.api.TxnResponse txnResponse) {
    super(txnResponse, txnResponse.getHeader());
  }

  /**
   * return true if the compare evaluated to true or false otherwise.
   */
  public boolean isSucceeded() {
    return getResponse().getSucceeded();
  }

  /**
   * returns a list of DeleteResponse; empty list if none.
   */
  public synchronized List<DeleteResponse> getDeleteResponses() {
    if (deleteResponses == null) {
      deleteResponses = getResponse().getResponsesList().stream()
          .filter((responseOp) -> responseOp.getResponseCase() == RESPONSE_DELETE_RANGE)
          .map(responseOp -> new DeleteResponse(responseOp.getResponseDeleteRange()))
          .collect(Collectors.toList());
    }

    return deleteResponses;
  }

  /**
   * returns a list of GetResponse; empty list if none.
   */
  public synchronized List<GetResponse> getGetResponses() {
    if (getResponses == null) {
      getResponses = getResponse().getResponsesList().stream()
          .filter((responseOp) -> responseOp.getResponseCase() == RESPONSE_RANGE)
          .map(responseOp -> new GetResponse(responseOp.getResponseRange()))
          .collect(Collectors.toList());
    }

    return getResponses;
  }

  /**
   * returns a list of PutResponse; empty list if none.
   */
  public synchronized List<PutResponse> getPutResponses() {
    if (putResponses == null) {
      putResponses = getResponse().getResponsesList().stream()
          .filter((responseOp) -> responseOp.getResponseCase() == RESPONSE_PUT)
          .map(responseOp -> new PutResponse(responseOp.getResponsePut()))
          .collect(Collectors.toList());
    }

    return putResponses;
  }
}
