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

package io.etcd.jetcd.launcher.junit4;

import java.net.URI;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit4 ExternalResource to have etcd cluster in tests.
 */
public class EtcdClusterResource extends ExternalResource implements EtcdCluster {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdClusterResource.class);

    private final EtcdCluster cluster;

    public EtcdClusterResource(String clusterName) {
        this(clusterName, 1, false);
    }

    public EtcdClusterResource(String clusterName, int nodes) {
        this(clusterName, nodes, false);
    }

    public EtcdClusterResource(String clusterName, int nodes, boolean ssl) {
        this(clusterName, nodes, ssl, false);
    }

    public EtcdClusterResource(String clusterName, int nodes, boolean ssl, boolean restartable) {
        this.cluster = EtcdClusterFactory.buildCluster(clusterName, nodes, ssl, restartable);
    }

    // Test framework methods

    @Override
    protected void before() throws Throwable {
        this.cluster.start();
    }

    @Override
    protected void after() {
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

    @NonNull
    @Override
    public List<URI> getClientEndpoints() {
        return this.cluster.getClientEndpoints();
    }

    @NonNull
    @Override
    public List<URI> getPeerEndpoints() {
        return this.cluster.getPeerEndpoints();
    }
}
