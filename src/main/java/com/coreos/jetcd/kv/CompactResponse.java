package com.coreos.jetcd.kv;

import com.coreos.jetcd.api.CompactionResponse;
import com.coreos.jetcd.data.AbstractResponse;

public class CompactResponse extends AbstractResponse<CompactionResponse> {

  public CompactResponse(CompactionResponse response) {
    super(response, response.getHeader());
  }
}
