package com.coreos.jetcd.data;

/**
 * Etcd key value pair.
 */
public class KeyValue {

  private com.coreos.jetcd.api.KeyValue kv;

  public KeyValue(com.coreos.jetcd.api.KeyValue kv) {
    this.kv = kv;
  }

  public ByteSequence getKey() {
    return ByteSequence.fromByteString(kv.getKey());
  }

  public ByteSequence getValue() {
    return ByteSequence.fromByteString(kv.getValue());
  }

  public long getCreateRevision() {
    return kv.getCreateRevision();
  }

  public long getModRevision() {
    return kv.getModRevision();
  }

  public long getVersion() {
    return kv.getVersion();
  }

  public long getLease() {
    return kv.getLease();
  }
}
