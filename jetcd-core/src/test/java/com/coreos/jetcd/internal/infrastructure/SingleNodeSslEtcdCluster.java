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

import com.coreos.jetcd.Client;
import com.coreos.jetcd.internal.impl.TestUtil;
import org.assertj.core.util.Lists;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static com.coreos.jetcd.internal.impl.TestConstants.ETCD_CLIENT_PORT;
import static com.coreos.jetcd.internal.impl.TestConstants.ETCD_DOCKER_IMAGE_NAME;

public class SingleNodeSslEtcdCluster implements EtcdCluster {
  private final GenericContainer nodeContainer;
  private final List<String> clientEndpoints;
  private final List<String> peerEndpoints;

  SingleNodeSslEtcdCluster(Network network) {
    nodeContainer = new GenericContainer(ETCD_DOCKER_IMAGE_NAME)
            .withExposedPorts(ETCD_CLIENT_PORT)
            .withClasspathResourceMapping("ssl/cert/server.pem", "/etc/ssl/etcd/server.pem", BindMode.READ_ONLY)
            .withClasspathResourceMapping("ssl/cert/server-key.pem", "/etc/ssl/etcd/server-key.pem", BindMode.READ_ONLY)
            .withNetwork(network)
            .withNetworkAliases("etcd-ssl")
            .withCommand(
                    "etcd " +
                            "--name etcd-ssl " +
                            "--cert-file=/etc/ssl/etcd/server.pem " +
                            "--key-file=/etc/ssl/etcd/server-key.pem " +
                            "--advertise-client-urls https://0.0.0.0:2379 " +
                            "--listen-client-urls https://0.0.0.0:2379"
            );
    nodeContainer.start();
    clientEndpoints = Lists.newArrayList(TestUtil.buildEndpoint(nodeContainer, "https", ETCD_CLIENT_PORT));
    peerEndpoints = TestUtil.buildPeerEndpoints(nodeContainer);
  }

  @Nonnull
  @Override
  public Client getClient() {
    return Client.builder().endpoints(clientEndpoints).build();
  }

  @Nonnull
  @Override
  public List<String> getClientEndpoints() {
    return Collections.unmodifiableList(clientEndpoints);
  }

  @Nonnull
  @Override
  public List<String> getPeerEndpoints() {
    return Collections.unmodifiableList(peerEndpoints);
  }

  @Override
  public void close() {
    nodeContainer.stop();
  }
}
