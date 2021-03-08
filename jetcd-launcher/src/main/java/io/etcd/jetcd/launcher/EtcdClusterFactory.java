/*
 * Copyright 2016-2021 The jetcd authors
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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.ResourceReaper;

import static java.util.stream.Collectors.toList;

public class EtcdClusterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdClusterFactory.class);

    public static EtcdCluster buildCluster(@NonNull
    String clusterName, int nodes, boolean ssl) {
        return buildCluster(clusterName, nodes, ssl, EtcdContainer.ETCD_DOCKER_IMAGE_NAME, Collections.emptyList());
    }

    public static EtcdCluster buildCluster(@NonNull
    String clusterName, int nodes, boolean ssl, String... additionalArgs) {
        return buildCluster(clusterName, nodes, ssl, EtcdContainer.ETCD_DOCKER_IMAGE_NAME, Arrays.asList(additionalArgs));
    }

    public static EtcdCluster buildCluster(
        @NonNull
        String clusterName,
        int nodes,
        boolean ssl,
        @NonNull
        String image,
        List<String> additionalArgs) {

        final Network network = Network.builder().id(clusterName).build();
        final CountDownLatch latch = new CountDownLatch(nodes);
        final AtomicBoolean failedToStart = new AtomicBoolean(false);
        final EtcdContainer.LifecycleListener listener = new EtcdContainer.LifecycleListener() {
            @Override
            public void started(EtcdContainer container) {
                latch.countDown();
            }

            @Override
            public void failedToStart(EtcdContainer container, Exception exception) {
                LOGGER.error("Exception while starting etcd container: ", exception);
                failedToStart.set(true);
                latch.countDown();
            }

            @Override
            public void stopped(EtcdContainer container) {
            }
        };

        final List<String> endpoints = IntStream.range(0, nodes).mapToObj(i -> "etcd" + i).collect(toList());

        final List<EtcdContainer> containers = endpoints.stream()
            .map(e -> new EtcdContainer(network, listener, ssl, clusterName, e, endpoints, image, additionalArgs))
            .collect(toList());

        ResourceReaper.instance().registerNetworkIdForCleanup(network.getId());

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
                if (failedToStart.get()) {
                    throw new IllegalStateException("Cluster failed to start");
                }
            }

            @Override
            public void close() {
                containers.forEach(EtcdContainer::close);

            }

            @Override
            public void restart() {
                containers.forEach(EtcdContainer::restart);
            }

            @NonNull
            @Override
            public List<URI> getClientEndpoints() {
                return containers.stream().map(EtcdContainer::clientEndpoint).collect(toList());
            }

            @NonNull
            @Override
            public List<URI> getPeerEndpoints() {
                return containers.stream().map(EtcdContainer::peerEndpoint).collect(toList());
            }
        };
    }
}
