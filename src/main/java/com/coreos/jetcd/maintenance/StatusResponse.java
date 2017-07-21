package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.data.AbstractResponse;

/**
 * StatusResponse returned by {@link Maintenance#statusMember(String)} contains
 * a header, version, dbSize, current leader, raftIndex, and raftTerm.
 */
public class StatusResponse extends AbstractResponse<com.coreos.jetcd.api.StatusResponse> {

  public StatusResponse(com.coreos.jetcd.api.StatusResponse response) {
    super(response, response.getHeader());
  }

  /**
   * returns the cluster protocol version used by the responding member.
   */
  public String getVersion() {
    return getResponse().getVersion();
  }

  /**
   * return the size of the backend database, in bytes, of the responding member.
   */
  public long getDbSize() {
    return getResponse().getDbSize();
  }

  /**
   * return the the member ID which the responding member believes is the current leader.
   */
  public long getLeader() {
    return getResponse().getLeader();
  }

  /**
   * the current raft index of the responding member.
   */
  public long getRaftIndex() {
    return getResponse().getRaftIndex();
  }

  /**
   * the current raft term of the responding member.
   */
  public long getRaftTerm() {
    return getResponse().getRaftTerm();
  }
}
