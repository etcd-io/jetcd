// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

public interface TxnResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:jetcd.TxnResponse)
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
   * <code>optional bool succeeded = 2;</code>
   *
   * <pre>
   * succeeded is set to true if the compare evaluated to true or false otherwise.
   * </pre>
   */
  boolean getSucceeded();

  /**
   * <code>repeated .jetcd.ResponseOp responses = 3;</code>
   *
   * <pre>
   * responses is a list of responses corresponding to the results from applying
   * success if succeeded is true or failure if succeeded is false.
   * </pre>
   */
  java.util.List<com.coreos.jetcd.ResponseOp> 
      getResponsesList();
  /**
   * <code>repeated .jetcd.ResponseOp responses = 3;</code>
   *
   * <pre>
   * responses is a list of responses corresponding to the results from applying
   * success if succeeded is true or failure if succeeded is false.
   * </pre>
   */
  com.coreos.jetcd.ResponseOp getResponses(int index);
  /**
   * <code>repeated .jetcd.ResponseOp responses = 3;</code>
   *
   * <pre>
   * responses is a list of responses corresponding to the results from applying
   * success if succeeded is true or failure if succeeded is false.
   * </pre>
   */
  int getResponsesCount();
  /**
   * <code>repeated .jetcd.ResponseOp responses = 3;</code>
   *
   * <pre>
   * responses is a list of responses corresponding to the results from applying
   * success if succeeded is true or failure if succeeded is false.
   * </pre>
   */
  java.util.List<? extends com.coreos.jetcd.ResponseOpOrBuilder> 
      getResponsesOrBuilderList();
  /**
   * <code>repeated .jetcd.ResponseOp responses = 3;</code>
   *
   * <pre>
   * responses is a list of responses corresponding to the results from applying
   * success if succeeded is true or failure if succeeded is false.
   * </pre>
   */
  com.coreos.jetcd.ResponseOpOrBuilder getResponsesOrBuilder(
      int index);
}
