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

package io.etcd.jetcd.resolver;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;

import com.google.common.base.Preconditions;

public abstract class AbstractNameResolver extends NameResolver {
    public static final int ETCD_CLIENT_PORT = 2379;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNameResolver.class);

    private final Object lock;
    private final String authority;
    private final URI targetUri;

    private volatile boolean shutdown;
    private volatile boolean resolving;

    @GuardedBy("lock")
    private Executor executor;
    @GuardedBy("lock")
    private Listener listener;

    public AbstractNameResolver(URI targetUri) {
        this.lock = new Object();
        this.targetUri = targetUri;
        this.authority = targetUri.getAuthority() != null ? targetUri.getAuthority() : "";
    }

    public URI getTargetUri() {
        return targetUri;
    }

    @Override
    public String getServiceAuthority() {
        return authority;
    }

    @Override
    public void start(Listener listener) {
        synchronized (lock) {
            Preconditions.checkState(this.listener == null, "already started");
            this.executor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);
            this.listener = Preconditions.checkNotNull(listener, "listener");
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

        synchronized (lock) {
            if (executor != null) {
                executor = SharedResourceHolder.release(GrpcUtil.SHARED_CHANNEL_EXECUTOR, executor);
            }
        }
    }

    private void resolve() {
        if (resolving || shutdown) {
            return;
        }
        synchronized (lock) {
            executor.execute(this::doResolve);
        }
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
            List<EquivalentAddressGroup> groups = computeAddressGroups();
            if (groups.isEmpty()) {
                throw EtcdExceptionFactory.newEtcdException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Unable to resolve endpoint " + targetUri);
            }

            savedListener.onAddresses(groups, Attributes.EMPTY);

        } catch (Exception e) {
            LOGGER.warn("Error wile getting list of servers", e);
            savedListener.onError(Status.NOT_FOUND);
        } finally {
            resolving = false;
        }
    }

    protected abstract List<EquivalentAddressGroup> computeAddressGroups();
}
