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
package com.coreos.jetcd.internal.infrastructure;

import org.testcontainers.containers.Network;

import javax.annotation.Nonnull;

public class ClusterFactory {
  @Nonnull
  public static EtcdCluster buildThreeNodeCluster(@Nonnull String clusterName) {
    return new ThreeNodeEtcdCluster(buildNetwork(clusterName));
  }

  @Nonnull
  public static EtcdCluster buildSingleNodeCluster(@Nonnull String clusterName) {
    return new SingleNodeEtcdCluster(buildNetwork(clusterName));
  }

  @Nonnull
  public static EtcdCluster buildSingleNodeClusterWithSsl(@Nonnull String clusterName) {
    return new SingleNodeSslEtcdCluster(buildNetwork(clusterName));
  }

  private static Network buildNetwork(String clusterName) {
    return Network.builder().id(clusterName).build();
  }
}
