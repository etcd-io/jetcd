/*
 * Copyright 2016-2023 The jetcd authors
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdContainer;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;

import com.google.common.base.Preconditions;

public class EtcdClusterNameResolver extends NameResolver {
    public static final String SCHEME = "cluster";
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdClusterNameResolver.class);

    private final Object lock;
    private final String authority;
    private final URI targetUri;

    private volatile boolean shutdown;
    private volatile boolean resolving;

    private Listener listener;

    public EtcdClusterNameResolver(URI targetUri) {
        this.lock = new Object();
        this.targetUri = targetUri;
        this.authority = targetUri.getAuthority();
    }

    @Override
    public String getServiceAuthority() {
        return authority;
    }

    @Override
    public void start(Listener listener) {
        synchronized (lock) {
            Preconditions.checkState(this.listener == null, "already started");
            this.listener = Objects.requireNonNull(listener, "listener");
            resolve();
        }
    }

    @Override
    public final synchronized void refresh() {
        resolve();
    }

    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
    }

    private void resolve() {
        if (resolving || shutdown) {
            return;
        }

        doResolve();
    }

    private void doResolve() {
        Listener savedListener;
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            resolving = true;
            savedListener = listener;
        }

        try {
            if (authority == null) {
                throw new RuntimeException("Unable to resolve endpoint " + targetUri);
            }

            EtcdCluster cluster = EtcdClusterExtension.cluster(authority);
            if (cluster == null) {
                throw new RuntimeException("Unable to find cluster " + authority);
            }

            List<EquivalentAddressGroup> servers = new ArrayList<>();

            for (EtcdContainer container : cluster.containers()) {
                try {
                    EquivalentAddressGroup ag = new EquivalentAddressGroup(
                        container.getClientAddress(),
                        Attributes.newBuilder()
                            .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, container.node())
                            .build());

                    servers.add(ag);
                } catch (IllegalStateException | IllegalArgumentException e) {
                    LOGGER.debug(
                        "Failure computing AddressGroup for cluster {}, {}",
                        cluster.clusterName(),
                        e.getMessage());
                }
            }

            if (!servers.isEmpty()) {
                savedListener.onAddresses(servers, Attributes.EMPTY);
            }

        } catch (Exception e) {
            LOGGER.warn("Error while getting list of servers", e);
            savedListener.onError(Status.NOT_FOUND);
        } finally {
            resolving = false;
        }
    }
}
