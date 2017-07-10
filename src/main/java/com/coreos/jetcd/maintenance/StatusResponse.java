package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.data.Header;

/**
 * StatusResponse returned by {@link Maintenance#statusMember(String)} contains
 * a header, version, dbSize, current leader, raftIndex, and raftTerm.
 */
public class StatusResponse {

  private final Header header;
  private final String version;
  private final long dbSize;
  private long leader;
  private long raftIndex;
  private long raftTerm;

  public StatusResponse(Header header, String version, long dbSize,
      long leader, long raftIndex, long raftTerm) {
    this.header = header;
    this.version = version;
    this.dbSize = dbSize;
    this.leader = leader;
    this.raftIndex = raftIndex;
    this.raftTerm = raftTerm;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * returns the cluster protocol version used by the responding member.
   */
  public String getVersion() {
    return version;
  }

  /**
   * return the size of the backend database, in bytes, of the responding member.
   */
  public long getDbSize() {
    return dbSize;
  }

  /**
   * return the the member ID which the responding member believes is the current leader.
   */
  public long getLeader() {
    return leader;
  }

  /**
   * the current raft index of the responding member.
   */
  public long getRaftIndex() {
    return raftIndex;
  }

  /**
   * the current raft term of the responding member.
   */
  public long getRaftTerm() {
    return raftTerm;
  }
}
