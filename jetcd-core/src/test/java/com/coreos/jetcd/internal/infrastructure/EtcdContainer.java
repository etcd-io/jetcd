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

import com.github.dockerjava.api.DockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EtcdContainer implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(EtcdCluster.class);

  private final String endpoint;
  private final boolean ssl;
  private final GenericContainer<?> container;
  private final EtcdCluster.LifecycleListener listener;

  public EtcdContainer(Network network, EtcdCluster.LifecycleListener listener, boolean ssl, String clusterName, String endpoint, List<String> endpoints) {
    this.endpoint = endpoint;
    this.ssl = ssl;
    this.listener = listener;

    final String name = endpoint;
    final List<String> command = new ArrayList<>();

    this.container = new GenericContainer<>(ETCD_DOCKER_IMAGE_NAME);
    this.container.withExposedPorts(ETCD_CLIENT_PORT, ETCD_PEER_PORT);
    this.container.withNetwork(network);
    this.container.withNetworkAliases(name);
    this.container.waitingFor(waitStrategy());
    this.container.withLogConsumer(logConsumer());

    command.add("etcd");
    command.add("--name");
    command.add(name);
    command.add("--advertise-client-urls");
    command.add((ssl ? "https" : "http") + "://0.0.0.0:" + ETCD_CLIENT_PORT);
    command.add("--listen-client-urls");
    command.add((ssl ? "https" : "http") + "://0.0.0.0:" + ETCD_CLIENT_PORT);

    if (ssl) {
      this.container.withClasspathResourceMapping(
        "ssl/cert/" + name + ".pem",
        "/etc/ssl/etcd/server.pem",
        BindMode.READ_ONLY,
        SelinuxContext.SHARED);

      this.container.withClasspathResourceMapping(
        "ssl/cert/" + name + "-key.pem",
        "/etc/ssl/etcd/server-key.pem",
        BindMode.READ_ONLY,
        SelinuxContext.SHARED);

      command.add("--cert-file");
      command.add("/etc/ssl/etcd/server.pem");
      command.add("--key-file");
      command.add("/etc/ssl/etcd/server-key.pem");
    }

    if (endpoints.size() > 1) {
      command.add("--initial-advertise-peer-urls");
      command.add("http://" + name + ":" + ETCD_PEER_PORT);
      command.add("--listen-peer-urls");
      command.add("http://0.0.0.0:" + ETCD_PEER_PORT);
      command.add("--initial-cluster-token");
      command.add(clusterName);
      command.add("--initial-cluster");
      command.add(endpoints.stream().map(e -> e + "=" + "http://" + e + ":" + ETCD_PEER_PORT).collect(Collectors.joining(",")));
      command.add("--initial-cluster-state");
      command.add("new");
    }

    if (!command.isEmpty()) {
      this.container.withCommand(command.toArray(new String[command.size()]));
    }
  }

  public void start() {
    LOGGER.debug("staring etcd container {} with command: {}", endpoint, String.join(" ", container.getCommandParts()));

    this.container.start();
    this.listener.started(this);

  }

  @Override
  public void close() {
    if (this.container != null) {
      this.container.stop();
    }
  }

  public String clientEndpoint() {
    final String host = container.getContainerIpAddress();
    final int port = container.getMappedPort(ETCD_CLIENT_PORT);

    return String.format("%s://%s:%d", ssl ? "https" : "http", host, port);
  }

  public String peerEndpoint() {
    final String host = container.getContainerIpAddress();
    final int port = container.getMappedPort(ETCD_PEER_PORT);

    return String.format("%s://%s:%d", ssl ? "https" : "http", host, port);
  }

  // ****************
  // helpers
  // ****************

  private WaitStrategy waitStrategy() {
    return new AbstractWaitStrategy() {
      @Override
      protected void waitUntilReady() {
        final DockerClient client = DockerClientFactory.instance().client();
        final WaitingConsumer waitingConsumer = new WaitingConsumer();

        LogUtils.followOutput(client, waitStrategyTarget.getContainerId(), waitingConsumer);

        try {
          waitingConsumer.waitUntil(
            f -> f.getUtf8String().contains("ready to serve client requests"),
            startupTimeout.getSeconds(),
            TimeUnit.SECONDS,
            1
          );
        } catch (TimeoutException e) {
          throw new ContainerLaunchException("Timed out");
        }
      }
    };
  }

  private Consumer<OutputFrame> logConsumer() {
    final Logger logger = LoggerFactory.getLogger(EtcdContainer.class);

    return outputFrame -> {
      final OutputFrame.OutputType outputType = outputFrame.getType();
      final String utf8String = outputFrame.getUtf8String().replaceAll("((\\r?\\n)|(\\r))$", "");

      switch (outputType) {
        case END:
          break;
        case STDOUT:
        case STDERR:
          logger.debug("{}{}: {}", endpoint, outputType, utf8String);
          break;
        default:
          throw new IllegalArgumentException("Unexpected outputType " + outputType);
      }
    };
  }
}
