/*
 * Copyright 2016-2020 The jetcd authors
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
import java.util.List;

import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit5 Extension to have etcd cluster in tests.
 */
public class EtcdClusterExtension implements EtcdCluster, BeforeAllCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdClusterExtension.class);

    private final EtcdCluster cluster;

    public EtcdClusterExtension(String clusterName, int nodes) {
        this(clusterName, nodes, false);
    }

    public EtcdClusterExtension(String clusterName, int nodes, boolean ssl) {
        this.cluster = EtcdClusterFactory.buildCluster(clusterName, nodes, ssl);
    }

    public EtcdClusterExtension(String clusterName, int nodes, boolean ssl, String... additionalArgs) {
        this.cluster = EtcdClusterFactory.buildCluster(clusterName, nodes, ssl, additionalArgs);
    }

    // Test framework methods

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        this.cluster.start();
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) throws Exception {
        try {
            this.cluster.close();
        } catch (RuntimeException e) {
            LOG.warn("close() failed (but ignoring it)", e);
        }
    }

    // Relay cluster methods to cluster

    @Override
    public void start() {
        this.cluster.start();
    }

    @Override
    public void restart() {
        this.cluster.restart();
    }

    @Override
    public void close() {
        this.cluster.close();
    }

    @Override
    public List<URI> getClientEndpoints() {
        return this.cluster.getClientEndpoints();
    }

    @Override
    public List<URI> getPeerEndpoints() {
        return this.cluster.getPeerEndpoints();
    }
}
