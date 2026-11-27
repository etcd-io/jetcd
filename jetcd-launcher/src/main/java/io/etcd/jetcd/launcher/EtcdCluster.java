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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.testcontainers.lifecycle.Startable;

/**
 * Represents a testcontainers-based etcd cluster for integration testing.
 * Provides lifecycle management and access to cluster endpoints.
 */
public interface EtcdCluster extends Startable {

    /**
     * Restarts all nodes in the cluster with a delay between restarts.
     * Useful for testing resilience, recovery, and failure scenarios.
     *
     * @param  delay                the delay between node restarts
     * @param  unit                 the time unit of the delay
     * @throws InterruptedException if the sleep is interrupted
     */
    default void restart(long delay, TimeUnit unit) throws InterruptedException {
        stop();

        if (delay > 0) {
            unit.sleep(delay);
        }

        start();
    }

    /**
     * Returns the cluster name used to identify this cluster instance.
     *
     * @return the cluster name
     */
    String clusterName();

    /**
     * Returns the client endpoints for connecting to the cluster.
     * These are the endpoints that clients should use to communicate with etcd.
     *
     * @return list of client endpoint URIs
     */
    List<URI> clientEndpoints();

    /**
     * Returns the peer endpoints used for cluster member communication.
     * These are used internally by etcd nodes for cluster coordination.
     *
     * @return list of peer endpoint URIs
     */
    List<URI> peerEndpoints();

    /**
     * Returns the individual container instances that make up the cluster.
     *
     * @return unmodifiable list of etcd containers
     */
    List<EtcdContainer> containers();
}
