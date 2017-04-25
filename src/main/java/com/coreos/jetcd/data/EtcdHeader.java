package com.coreos.jetcd.data;

/**
 * Etcd message header information.
 */
public class EtcdHeader {
    private final long clusterId;
    private final long memberId;
    private final long revision;
    private final long raftTerm;
    private final long compactRevision;

    public EtcdHeader(long clusterId, long memberId, long revision, long raftTerm, long compactRevision) {
        this.clusterId = clusterId;
        this.memberId = memberId;
        this.revision = revision;
        this.raftTerm = raftTerm;
        this.compactRevision = compactRevision;
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

    public long getCompactRevision() {
        return compactRevision;
    }
}
