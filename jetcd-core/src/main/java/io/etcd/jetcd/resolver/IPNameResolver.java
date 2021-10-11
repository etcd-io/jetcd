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
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPNameResolver extends NameResolver {
    public static final String SCHEME = "ip";

    private static final Logger LOGGER = LoggerFactory.getLogger(IPNameResolver.class);
    private static final String ETCD_CLIENT_PORT = "2379";

    private final Object lock;
    private final String authority;
    private final URI targetUri;
    private final List<HostAndPort> addresses;

    private volatile boolean shutdown;
    private volatile boolean resolving;

    @GuardedBy("lock")
    private Executor executor;
    @GuardedBy("lock")
    private Listener listener;

    public IPNameResolver(URI targetUri) {
        this.lock = new Object();
        this.targetUri = targetUri;
        this.authority = targetUri.getAuthority() != null ? targetUri.getAuthority() : "";
        this.addresses = Stream.of(targetUri.getPath().split(","))
            .map(address -> address.startsWith("/") ? address.substring(1) : address)
            .map(HostAndPort::new)
            .collect(Collectors.toList());
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

            List<EquivalentAddressGroup> servers = addresses.stream()
                .map(address -> address.toAddressGroup(authority))
                .collect(Collectors.toList());

            savedListener.onAddresses(servers, Attributes.EMPTY);

        } catch (Exception e) {
            LOGGER.warn("Error wile getting list of servers", e);
            savedListener.onError(Status.NOT_FOUND);
        } finally {
            resolving = false;
        }
    }

    private static final class HostAndPort {
        final String host;
        final int port;

        public HostAndPort(String address) {
            final Iterable<String> split = Splitter.on(':').split(address);

            this.host = Iterables.get(split, 0);
            this.port = Integer.parseInt(Iterables.get(split, 1, ETCD_CLIENT_PORT));
        }

        public String authority() {
            return String.format("%s:%d", host, port);
        }

        public EquivalentAddressGroup toAddressGroup(String authority) {
            return new EquivalentAddressGroup(
                new InetSocketAddress(host, port),
                Strings.isNullOrEmpty(authority)
                    ? Attributes.newBuilder()
                        .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, authority())
                        .build()
                    : Attributes.EMPTY);
        }
    }
}
