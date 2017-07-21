package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.data.AbstractResponse;

/**
 * DefragmentResponse returned by {@link Maintenance#defragmentMember(String)} contains a header.
 */
public class DefragmentResponse extends AbstractResponse<com.coreos.jetcd.api.DefragmentResponse> {

  public DefragmentResponse(com.coreos.jetcd.api.DefragmentResponse response) {
    super(response, response.getHeader());
  }
}
