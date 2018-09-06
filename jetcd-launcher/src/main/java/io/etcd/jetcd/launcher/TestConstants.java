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

package io.etcd.jetcd.launcher;

/**
 * Test constants, contain the cluster info.
 * @deprecated This will be removed.
 */
@Deprecated
public class TestConstants {
  public static final String ETCD_DOCKER_IMAGE_NAME = "gcr.io/etcd-development/etcd:v3.3";

  public static final int ETCD_CLIENT_PORT = 2379;
  public static final int ETCD_PEER_PORT = 2380;

  public static final String DEFAULT_SSL_AUTHORITY = "etcd0";

  public static final String DEFAULT_SSL_CA_PATH = "/ssl/cert/ca.pem";
}
