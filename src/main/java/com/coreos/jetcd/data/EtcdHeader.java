package com.coreos.jetcd.data;

/**
 * Etcd message header information.
 */
public class EtcdHeader {
    private final long clusterId;
    private final long memberId;
    private final long revision;
    private final long raftTerm;

    public EtcdHeader(long clusterId, long memberId, long revision, long raftTerm) {
        this.clusterId = clusterId;
        this.memberId = memberId;
        this.revision = revision;
        this.raftTerm = raftTerm;
    }

    public long getClusterId() {
        return clusterId;
    }

    public long getMemberId() {
        return memberId;
    }

    public long getRevision() {
        return revision;
    }

    public long getRaftTerm() {
        return raftTerm;
    }
}
