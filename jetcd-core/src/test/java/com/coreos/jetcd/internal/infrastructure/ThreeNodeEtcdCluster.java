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

import static com.coreos.jetcd.internal.impl.TestConstants.ETCD_CLIENT_PORT;
import static com.coreos.jetcd.internal.impl.TestConstants.ETCD_DOCKER_IMAGE_NAME;
import static com.coreos.jetcd.internal.impl.TestConstants.ETCD_PEER_PORT;

import com.coreos.jetcd.Client;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class ThreeNodeEtcdCluster implements EtcdCluster {
  private final GenericContainer firstNodeContainer;
  private final GenericContainer secondNodeContainer;
  private final GenericContainer thirdNodeContainer;

  public ThreeNodeEtcdCluster(Network network) {
    firstNodeContainer = createClusterNode(network, 1);
    secondNodeContainer = createClusterNode(network, 2);
    thirdNodeContainer = createClusterNode(network, 3);

    firstNodeContainer.start();
    secondNodeContainer.start();
    thirdNodeContainer.start();
  }

  @Nonnull
  @Override
  public Client getClient() {
    return Client.builder().endpoints(getClientEndpoints()).build();
  }

  @Nonnull
  @Override
  public List<GenericContainer> getContainers() {
    return Arrays.asList(firstNodeContainer, secondNodeContainer, thirdNodeContainer);
  }

  @Override
  public void close() {
    firstNodeContainer.stop();
    secondNodeContainer.stop();
    thirdNodeContainer.stop();
  }

  private static GenericContainer createClusterNode(Network network, int nodeNumber) {
    return new GenericContainer(ETCD_DOCKER_IMAGE_NAME)
      .withExposedPorts(ETCD_CLIENT_PORT, ETCD_PEER_PORT)
      .withNetwork(network)
      .withNetworkAliases("etcd" + nodeNumber)
      .withCommand(
        "etcd "
          + "-name etcd" + nodeNumber + " "
          + "-advertise-client-urls http://0.0.0.0:2379 "
          + "-listen-client-urls http://0.0.0.0:2379 "
          + "-initial-advertise-peer-urls http://etcd" + nodeNumber + ":2380 "
          + "-listen-peer-urls http://0.0.0.0:2380 "
          + "-initial-cluster-token etcd-cluster "
          + "-initial-cluster etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380 "
          + "-initial-cluster-state new"
      );
  }
}
