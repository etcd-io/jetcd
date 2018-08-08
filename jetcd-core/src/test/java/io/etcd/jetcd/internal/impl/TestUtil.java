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
package io.etcd.jetcd.internal.impl;

import io.etcd.jetcd.data.ByteSequence;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.testcontainers.containers.GenericContainer;

public class TestUtil {

  public static String randomString() {
    return java.util.UUID.randomUUID().toString();
  }

  public static ByteSequence randomByteSequence() {
    return ByteSequence.fromString(randomString());
  }

  public static int findNextAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  public static String buildClientEndpoint(GenericContainer container) {
    return buildEndpoint(container, "http", TestConstants.ETCD_CLIENT_PORT);
  }

  public static List<String> buildClientEndpoints(GenericContainer... etcdContainers) {
    return Arrays.stream(etcdContainers)
            .map(TestUtil::buildClientEndpoint)
            .collect(Collectors.toList());
  }

  public static String buildPeerEndpoint(GenericContainer container) {
    return buildEndpoint(container, "http", TestConstants.ETCD_PEER_PORT);
  }

  public static List<String> buildPeerEndpoints(GenericContainer... etcdContainers) {
    return Arrays.stream(etcdContainers)
            .map(TestUtil::buildPeerEndpoint)
            .collect(Collectors.toList());
  }

  public static String buildEndpoint(GenericContainer container, String scheme, int port) {
    String nodeAddress = container.getContainerIpAddress();
    Integer nodePort = container.getMappedPort(port);
    return scheme + "://" + nodeAddress + ":" + nodePort;
  }
}
