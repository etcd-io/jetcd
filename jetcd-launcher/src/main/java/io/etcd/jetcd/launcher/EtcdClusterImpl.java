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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.testcontainers.containers.Network;

import static java.util.stream.Collectors.toList;

public class EtcdClusterImpl implements EtcdCluster {
    private final List<EtcdContainer> containers;
    private final String clusterName;
    private final List<String> endpoints;

    public EtcdClusterImpl(
        String image,
        String clusterName,
        String prefix,
        int nodes,
        boolean ssl,
        Collection<String> additionalArgs,
        Network network,
        boolean shouldMountDataDirectory) {

        this.clusterName = clusterName;
        this.endpoints = IntStream.range(0, nodes)
            .mapToObj(i -> (prefix == null ? "etcd" : prefix + "etcd") + i)
            .collect(toList());

        this.containers = endpoints.stream()
            .map(e -> new EtcdContainer(image, e, endpoints)
                .withClusterToken(clusterName)
                .withSll(ssl)
                .withAdditionalArgs(additionalArgs)
                .withNetwork(network)
                .withShouldMountDataDirectory(shouldMountDataDirectory))
            .collect(toList());
    }

    @Override
    public void start() {
        final CountDownLatch latch = new CountDownLatch(containers.size());
        final AtomicReference<Exception> failedToStart = new AtomicReference<>();

        for (EtcdContainer container : containers) {
            new Thread(() -> {
                try {
                    container.start();
                } catch (Exception e) {
                    failedToStart.set(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (failedToStart.get() != null) {
            throw new IllegalStateException("Cluster failed to start", failedToStart.get());
        }
    }

    @Override
    public void stop() {
        for (EtcdContainer container : containers) {
            container.stop();
        }
    }

    @Override
    public void close() {
        for (EtcdContainer container : containers) {
            container.close();
        }
    }

    @Override
    public String clusterName() {
        return clusterName;
    }

    @Override
    public List<URI> clientEndpoints() {
        return containers.stream().map(EtcdContainer::clientEndpoint).collect(toList());
    }

    @Override
    public List<URI> peerEndpoints() {
        return containers.stream().map(EtcdContainer::peerEndpoint).collect(toList());
    }

    @Override
    public List<EtcdContainer> containers() {
        return Collections.unmodifiableList(containers);
    }
}
