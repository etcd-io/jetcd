package com.coreos.jetcd.lease;

import com.coreos.jetcd.EtcdLease;
import com.coreos.jetcd.api.LeaseGrantResponse;
import com.coreos.jetcd.data.EtcdHeader;

/**
 * The Lease hold the keepAlive information for lease
 */
public class Lease {

    public final EtcdHeader header;

    private final long                 leaseID;

    private final long                       ttl;

    public Lease(long leaseID, long ttl, EtcdHeader header) {
        this.header = header;
        this.leaseID = leaseID;
        this.ttl = ttl;
    }

    public long getLeaseID() {
        return leaseID;
    }

    public long getTtl() {
        return ttl;
    }
}
