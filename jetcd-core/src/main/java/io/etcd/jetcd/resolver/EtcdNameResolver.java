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

package io.etcd.jetcd.resolver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.concurrent.GuardedBy;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtcdNameResolver extends NameResolver {
    public static final String SCHEME = "etcd";

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdNameResolver.class);
    private static final String ETCD_CLIENT_PORT = "2379";

    private final Object lock;
    private final String authority;
    private final URI targetUri;
    private final List<EquivalentAddressGroup> addresses;

    private volatile boolean shutdown;
    private volatile boolean resolving;

    @GuardedBy("lock")
    private Executor executor;
    @GuardedBy("lock")
    private Listener listener;

    public EtcdNameResolver(URI targetUri) {
        this.lock = new Object();
        this.targetUri = targetUri;
        this.authority = targetUri.getAuthority() != null ? targetUri.getAuthority() : SCHEME;
        this.addresses = Stream.of(targetUri.getPath().split(","))
            .map(address -> {
                return address.startsWith("/")
                    ? address.substring(1)
                    : address;
            })
            .map(address -> {
                Iterable<String> split = Splitter.on(':').split(address);
                String host = Iterables.get(split, 0);
                String port = Iterables.get(split, 1, ETCD_CLIENT_PORT);
                return new InetSocketAddress(host, Integer.parseInt(port));
            }).map(address -> {
                return new EquivalentAddressGroup(
                    address,
                    Strings.isNullOrEmpty(authority)
                        ? io.grpc.Attributes.newBuilder()
                            .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, targetUri.getAuthority())
                            .build()
                        : io.grpc.Attributes.EMPTY);
            }).collect(Collectors.toList());
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
            if (addresses.isEmpty()) {
                throw EtcdExceptionFactory.newEtcdException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Unable to resolve endpoint " + targetUri);
            }

            savedListener.onAddresses(addresses, io.grpc.Attributes.EMPTY);
        } catch (Exception e) {
            LOGGER.warn("Error wile getting list of servers", e);
            savedListener.onError(Status.NOT_FOUND);
        } finally {
            resolving = false;
        }
    }
}
