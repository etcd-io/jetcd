package com.coreos.jetcd.lease;

import com.coreos.jetcd.Lease.LeaseHandler;
import com.coreos.jetcd.api.LeaseGrantResponse;

/**
 * The KeepAlive hold the keepAlive information for lease.
 */
public class KeepAlive {

  private final long leaseID;

  private long ttl;

  private LeaseGrantResponse leaseGrantResponse;

  private long deadLine;

  private long nextKeepAlive;

  private LeaseHandler leaseHandler;

  public KeepAlive(long leaseID) {
    this(leaseID, null);
  }

  public KeepAlive(long leaseID, LeaseHandler leaseHandler) {
    this.leaseID = leaseID;
    this.leaseHandler = leaseHandler;
  }

  public long getLeaseID() {
    return leaseID;
  }

  public long getDeadLine() {
    return deadLine;
  }

  public KeepAlive setDeadLine(long deadLine) {
    this.deadLine = deadLine;
    return this;
  }

  public long getNextKeepAlive() {
    return nextKeepAlive;
  }

  public KeepAlive setNextKeepAlive(long nextKeepAlive) {
    this.nextKeepAlive = nextKeepAlive;
    return this;
  }

  public boolean isContainHandler() {
    return leaseHandler != null;
  }

  public LeaseHandler getLeaseHandler() {
    return leaseHandler;
  }

  public void setLeaseHandler(LeaseHandler leaseHandler) {
    this.leaseHandler = leaseHandler;
  }
}
