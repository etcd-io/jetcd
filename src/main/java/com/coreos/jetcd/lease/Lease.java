package com.coreos.jetcd.lease;

import com.coreos.jetcd.EtcdLease;
import com.coreos.jetcd.api.LeaseGrantResponse;

/**
 * The Lease hold the keepAlive information for lease
 */
public class Lease {

  private final long leaseID;

  private long ttl;

  private LeaseGrantResponse leaseGrantResponse;

  private long deadLine;

  private long nextKeepAlive;

  private EtcdLease.EtcdLeaseHandler etcdLeaseHandler;

  public Lease(long leaseID) {
    this(leaseID, null);
  }

  public Lease(long leaseID, EtcdLease.EtcdLeaseHandler etcdLeaseHandler) {
    this.leaseID = leaseID;
    this.etcdLeaseHandler = etcdLeaseHandler;
  }

  public long getLeaseID() {
    return leaseID;
  }

  public long getDeadLine() {
    return deadLine;
  }

  public Lease setDeadLine(long deadLine) {
    this.deadLine = deadLine;
    return this;
  }

  public long getNextKeepAlive() {
    return nextKeepAlive;
  }

  public Lease setNextKeepAlive(long nextKeepAlive) {
    this.nextKeepAlive = nextKeepAlive;
    return this;
  }

  public boolean isContainHandler() {
    return etcdLeaseHandler != null;
  }

  public EtcdLease.EtcdLeaseHandler getEtcdLeaseHandler() {
    return etcdLeaseHandler;
  }

  public void setEtcdLeaseHandler(EtcdLease.EtcdLeaseHandler etcdLeaseHandler) {
    this.etcdLeaseHandler = etcdLeaseHandler;
  }
}
