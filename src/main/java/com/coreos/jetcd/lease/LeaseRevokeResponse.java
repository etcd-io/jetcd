package com.coreos.jetcd.lease;

import com.coreos.jetcd.data.AbstractResponse;

public class LeaseRevokeResponse extends
    AbstractResponse<com.coreos.jetcd.api.LeaseRevokeResponse> {

  public LeaseRevokeResponse(com.coreos.jetcd.api.LeaseRevokeResponse revokeResponse) {
    super(revokeResponse, revokeResponse.getHeader());
  }
}
