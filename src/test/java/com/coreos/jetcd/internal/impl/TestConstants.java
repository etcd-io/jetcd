package com.coreos.jetcd.internal.impl;

/**
 * Test constants, contain the cluster info.
 */
public class TestConstants {

  public static final String[] endpoints = System.getProperty(
      "etcd.endpoints",
      "http://localhost:2379,http://localhost:22379,http://localhost:32379").split(",");

  public static final String[] peerUrls =  System.getProperty(
      "etcd.peerurls",
      "http://localhost:12380,http://localhost:22380,http://localhost:32380").split(",");
}
