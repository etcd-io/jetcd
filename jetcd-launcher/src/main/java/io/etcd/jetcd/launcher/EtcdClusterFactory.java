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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.testcontainers.containers.Network;


public class EtcdClusterFactory {

  public static EtcdCluster buildCluster(@Nonnull String clusterName, int nodes, boolean ssl) {
    final Network network = Network.builder().id(clusterName).build();
    final CountDownLatch latch = new CountDownLatch(nodes);
    final EtcdCluster.LifecycleListener listener = new EtcdCluster.LifecycleListener() {
      @Override
      public void started(EtcdContainer container) {
        latch.countDown();
      }

      @Override
      public void stopped(EtcdContainer container) {
      }
    };

    final List<String> endpoints = IntStream.range(0, nodes).mapToObj(i -> "etcd" + i).collect(toList());

    final List<EtcdContainer> containers = endpoints.stream()
                .map(e -> new EtcdContainer(network, listener, ssl, clusterName, e, endpoints)).collect(toList());

    return new EtcdCluster() {
      @Override
      public void start() {
        for (EtcdContainer container : containers) {
          new Thread(container::start).start();
        }

        try {
          latch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void close() {
        containers.forEach(EtcdContainer::close);
      }

      @Nonnull
      @Override
      public List<String> getClientEndpoints() {
        return containers.stream().map(EtcdContainer::clientEndpoint).collect(toList());
      }

      @Nonnull
      @Override
      public List<String> getPeerEndpoints() {
        return containers.stream().map(EtcdContainer::peerEndpoint).collect(toList());
      }
    };
  }
}
