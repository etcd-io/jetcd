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

package io.etcd.jetcd.test;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;

import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdCluster;

/**
 * JUnit5 Extension to have etcd cluster in tests.
 */
public class EtcdClusterExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    private static final Map<String, EtcdCluster> CLUSTERS = new ConcurrentHashMap<>();

    private final EtcdCluster cluster;
    private final AtomicBoolean beforeAll;

    private EtcdClusterExtension(EtcdCluster cluster) {
        this.cluster = cluster;
        this.beforeAll = new AtomicBoolean();
    }

    public EtcdCluster cluster() {
        return this.cluster;
    }

    public void restart() {
        this.cluster.restart();
    }

    public String clusterName() {
        return this.cluster.clusterName();
    }

    public List<URI> clientEndpoints() {
        return this.cluster.clientEndpoints();
    }

    public List<URI> peerEndpoints() {
        return this.cluster.peerEndpoints();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        this.beforeAll.set(true);
        before(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        before(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        this.beforeAll.set(false);
        after(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        after(context);
    }

    protected synchronized void before(ExtensionContext context) {
        EtcdCluster oldCluster = CLUSTERS.putIfAbsent(cluster.clusterName(), cluster);
        if (oldCluster == null) {
            cluster.start();
        }
    }

    protected synchronized void after(ExtensionContext context) {
        if (!this.beforeAll.get()) {
            try {
                cluster.close();
            } finally {
                CLUSTERS.remove(cluster.clusterName());
            }
        }
    }

    public static EtcdCluster cluster(String clusterName) {
        return CLUSTERS.get(clusterName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Etcd.Builder builder = new Etcd.Builder();

        public Builder withClusterName(String clusterName) {
            builder.withClusterName(clusterName);
            return this;
        }

        public Builder withPrefix(String prefix) {
            builder.withPrefix(prefix);
            return this;
        }

        public Builder withNodes(int nodes) {
            builder.withNodes(nodes);
            return this;
        }

        public Builder withSsl(boolean ssl) {
            builder.withSsl(ssl);
            return this;
        }

        public Builder withAdditionalArgs(Collection<String> additionalArgs) {
            builder.withAdditionalArgs(additionalArgs);
            return this;
        }

        public Builder withImage(String image) {
            builder.withImage(image);
            return this;
        }

        public Builder withNetwork(Network network) {
            builder.withNetwork(network);
            return this;
        }

        public Builder withMountDirectory(boolean mountDirectory) {
            builder.withMountedDataDirectory(mountDirectory);
            return this;
        }

        public EtcdClusterExtension build() {
            return new EtcdClusterExtension(builder.build());
        }
    }
}
