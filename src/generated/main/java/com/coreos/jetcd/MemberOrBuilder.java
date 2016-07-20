// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

public interface MemberOrBuilder extends
    // @@protoc_insertion_point(interface_extends:jetcd.Member)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional uint64 ID = 1;</code>
   *
   * <pre>
   * ID is the member ID for this member.
   * </pre>
   */
  long getID();

  /**
   * <code>optional string name = 2;</code>
   *
   * <pre>
   * name is the human-readable name of the member. If the member is not started, the name will be an empty string.
   * </pre>
   */
  java.lang.String getName();
  /**
   * <code>optional string name = 2;</code>
   *
   * <pre>
   * name is the human-readable name of the member. If the member is not started, the name will be an empty string.
   * </pre>
   */
  com.google.protobuf.ByteString
      getNameBytes();

  /**
   * <code>repeated string peerURLs = 3;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the member exposes to the cluster for communication.
   * </pre>
   */
  com.google.protobuf.ProtocolStringList
      getPeerURLsList();
  /**
   * <code>repeated string peerURLs = 3;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the member exposes to the cluster for communication.
   * </pre>
   */
  int getPeerURLsCount();
  /**
   * <code>repeated string peerURLs = 3;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the member exposes to the cluster for communication.
   * </pre>
   */
  java.lang.String getPeerURLs(int index);
  /**
   * <code>repeated string peerURLs = 3;</code>
   *
   * <pre>
   * peerURLs is the list of URLs the member exposes to the cluster for communication.
   * </pre>
   */
  com.google.protobuf.ByteString
      getPeerURLsBytes(int index);

  /**
   * <code>repeated string clientURLs = 4;</code>
   *
   * <pre>
   * clientURLs is the list of URLs the member exposes to clients for communication. If the member is not started, clientURLs will be empty.
   * </pre>
   */
  com.google.protobuf.ProtocolStringList
      getClientURLsList();
  /**
   * <code>repeated string clientURLs = 4;</code>
   *
   * <pre>
   * clientURLs is the list of URLs the member exposes to clients for communication. If the member is not started, clientURLs will be empty.
   * </pre>
   */
  int getClientURLsCount();
  /**
   * <code>repeated string clientURLs = 4;</code>
   *
   * <pre>
   * clientURLs is the list of URLs the member exposes to clients for communication. If the member is not started, clientURLs will be empty.
   * </pre>
   */
  java.lang.String getClientURLs(int index);
  /**
   * <code>repeated string clientURLs = 4;</code>
   *
   * <pre>
   * clientURLs is the list of URLs the member exposes to clients for communication. If the member is not started, clientURLs will be empty.
   * </pre>
   */
  com.google.protobuf.ByteString
      getClientURLsBytes(int index);
}
