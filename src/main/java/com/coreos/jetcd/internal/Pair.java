package com.coreos.jetcd.internal;

public class Pair<K, V> {

  final K key;
  final V value;

  public Pair(K k, V v) {
    this.key = k;
    this.value = v;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return key.hashCode() + value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Pair)) {
      return false;
    }
    Pair<?, ?> other = (Pair<?, ?>) obj;
    return key.equals(other.key) && value.equals(other.value);
  }
}
