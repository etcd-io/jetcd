// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

public interface StatusResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:jetcd.StatusResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional .jetcd.ResponseHeader header = 1;</code>
   */
  boolean hasHeader();
  /**
   * <code>optional .jetcd.ResponseHeader header = 1;</code>
   */
  com.coreos.jetcd.ResponseHeader getHeader();
  /**
   * <code>optional .jetcd.ResponseHeader header = 1;</code>
   */
  com.coreos.jetcd.ResponseHeaderOrBuilder getHeaderOrBuilder();

  /**
   * <code>optional string version = 2;</code>
   *
   * <pre>
   * version is the cluster protocol version used by the responding member.
   * </pre>
   */
  java.lang.String getVersion();
  /**
   * <code>optional string version = 2;</code>
   *
   * <pre>
   * version is the cluster protocol version used by the responding member.
   * </pre>
   */
  com.google.protobuf.ByteString
      getVersionBytes();

  /**
   * <code>optional int64 dbSize = 3;</code>
   *
   * <pre>
   * dbSize is the size of the backend database, in bytes, of the responding member.
   * </pre>
   */
  long getDbSize();

  /**
   * <code>optional uint64 leader = 4;</code>
   *
   * <pre>
   * leader is the member ID which the responding member believes is the current leader.
   * </pre>
   */
  long getLeader();

  /**
   * <code>optional uint64 raftIndex = 5;</code>
   *
   * <pre>
   * raftIndex is the current raft index of the responding member.
   * </pre>
   */
  long getRaftIndex();

  /**
   * <code>optional uint64 raftTerm = 6;</code>
   *
   * <pre>
   * raftTerm is the current raft term of the responding member.
   * </pre>
   */
  long getRaftTerm();
}
