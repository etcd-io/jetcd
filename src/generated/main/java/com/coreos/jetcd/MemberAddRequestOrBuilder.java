// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

public interface MemberAddRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:jetcd.MemberAddRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated string peerURLs = 1;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the added member will use to communicate with the cluster.
   * </pre>
   */
  com.google.protobuf.ProtocolStringList
      getPeerURLsList();
  /**
   * <code>repeated string peerURLs = 1;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the added member will use to communicate with the cluster.
   * </pre>
   */
  int getPeerURLsCount();
  /**
   * <code>repeated string peerURLs = 1;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the added member will use to communicate with the cluster.
   * </pre>
   */
  java.lang.String getPeerURLs(int index);
  /**
   * <code>repeated string peerURLs = 1;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the added member will use to communicate with the cluster.
   * </pre>
   */
  com.google.protobuf.ByteString
      getPeerURLsBytes(int index);
}
