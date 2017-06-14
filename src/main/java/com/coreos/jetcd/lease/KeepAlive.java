package com.coreos.jetcd.lease;

import com.coreos.jetcd.LeaseImpl.KeepAliveListenerImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The KeepAlive hold the keepAlive information for lease.
 */
public class KeepAlive {

  private long deadLine;

  private long nextKeepAlive;

  // ownerLock protects owner map.
  private final Object ownerLock;

  private Map<Long, KeepAlive> owner;

  private long leaseId;

  private Set<KeepAliveListenerImpl> listenersSet = Collections
      .newSetFromMap(new ConcurrentHashMap<>());

  public KeepAlive(Map<Long, KeepAlive> owner, Object ownerLock, long leaseId) {
    this.owner = owner;
    this.ownerLock = ownerLock;
    this.leaseId = leaseId;
  }

  public long getDeadLine() {
    return deadLine;
  }

  public void setDeadLine(long deadLine) {
    this.deadLine = deadLine;
  }

  public void addListener(KeepAliveListenerImpl listener) {
    this.listenersSet.add(listener);
  }

  public long getNextKeepAlive() {
    return nextKeepAlive;
  }

  public void setNextKeepAlive(long nextKeepAlive) {
    this.nextKeepAlive = nextKeepAlive;
  }


  public void sentKeepAliveResp(LeaseKeepAliveResponseWithError lkae) {
    this.listenersSet.forEach((l) -> l.enqueue(lkae));
  }

  public void removeListener(KeepAliveListenerImpl l) {
    this.listenersSet.remove(l);
    synchronized (this.ownerLock) {
      if (this.listenersSet.isEmpty()) {
        this.owner.remove(this.leaseId);
      }
    }
  }

  public void close() {
    this.listenersSet.forEach((l) -> l.close());
    this.listenersSet.clear();
  }
}
