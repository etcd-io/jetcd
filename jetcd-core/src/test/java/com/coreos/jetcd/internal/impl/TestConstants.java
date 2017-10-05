/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coreos.jetcd.internal.impl;

/**
 * Test constants, contain the cluster info.
 */
public class TestConstants {

  public static final String[] endpoints = System.getProperty(
      "etcd.endpoints",
      "http://localhost:12379,http://localhost:22379,http://localhost:32379").split(",");

  public static final String[] peerUrls =  System.getProperty(
      "etcd.peerurls",
      "http://localhost:12380,http://localhost:22380,http://localhost:32380").split(",");

  public static final String DEFAULT_SSL_AUTHORITY = "etcd-ssl";

  public static final String DEFAULT_SSL_ENDPOINTS = "https://127.0.0.1:42379";

  public static final String DEFAULT_SSL_CA_PATH = "/ssl/cert/ca.pem";
}
