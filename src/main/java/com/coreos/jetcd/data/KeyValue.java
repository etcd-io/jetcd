package com.coreos.jetcd.data;

/**
 * Etcd key value pair.
 */
public class KeyValue {

    private final ByteSequence key;
    private final ByteSequence value;
    private long createRevision = 0L;
    private long modRevision = 0L;
    private long version = 0L;
    private long lease = 0L;

    public KeyValue(ByteSequence key, ByteSequence value, long createRevision, long modRevision, long version, long lease) {
        this.key = key;
        this.value = value;
        this.createRevision = createRevision;
        this.modRevision = modRevision;
        this.version = version;
        this.lease = lease;
    }

    public ByteSequence getKey() {
        return key;
    }

    public ByteSequence getValue() {
        return value;
    }

    public long getCreateRevision() {
        return createRevision;
    }

    public long getModRevision() {
        return modRevision;
    }

    public long getVersion() {
        return version;
    }

    public long getLease() {
        return lease;
    }
}
