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
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.GuardedBy;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsSrvNameResolver extends NameResolver {
    public static final String SCHEME = "dns+srv";

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsSrvNameResolver.class);

    private static final String[] ATTRIBUTE_IDS;
    private static final Hashtable<String, String> ENV;

    static {
        ATTRIBUTE_IDS = new String[] { "SRV" };

        ENV = new Hashtable<>();
        ENV.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        ENV.put("java.naming.provider.url", "dns:");
    }

    private final Object lock;
    private final String authority;
    private final URI targetUri;

    private volatile boolean shutdown;
    private volatile boolean resolving;

    @GuardedBy("lock")
    private Executor executor;
    @GuardedBy("lock")
    private Listener listener;

    public DnsSrvNameResolver(URI targetUri) {
        this.lock = new Object();
        this.targetUri = targetUri;
        this.authority = targetUri.getAuthority() != null ? targetUri.getAuthority() : SCHEME;
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
            List<EquivalentAddressGroup> groups = new ArrayList<>();

            for (SocketAddress address : resolveAddresses()) {
                //
                // if the authority is not explicit set on the builder
                // then it will be computed from the URI
                //
                groups.add(new EquivalentAddressGroup(
                    address,
                    io.grpc.Attributes.EMPTY));
            }

            if (groups.isEmpty()) {
                throw EtcdExceptionFactory.newEtcdException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Unable to resolve endpoint " + targetUri);
            }

            savedListener.onAddresses(groups, io.grpc.Attributes.EMPTY);
        } catch (Exception e) {
            LOGGER.warn("Error wile getting list of servers", e);
            savedListener.onError(Status.NOT_FOUND);
        } finally {
            resolving = false;
        }
    }

    private List<SocketAddress> resolveAddresses() {
        List<SocketAddress> addresses = new LinkedList<>();

        try {
            String address = targetUri.getPath();
            if (address.startsWith("/")) {
                address = address.substring(1);
            }

            DirContext ctx = new InitialDirContext(ENV);
            Attributes attributes = ctx.getAttributes(address, ATTRIBUTE_IDS);
            NamingEnumeration<?> resolved = attributes.get("srv").getAll();

            while (resolved.hasMore()) {
                String record = (String) resolved.next();
                List<String> split = Splitter.on(' ').splitToList(record);

                if (split.size() >= 4) {
                    String host = split.get(3).trim();
                    String port = split.get(2).trim();

                    addresses.add(new InetSocketAddress(host, Integer.parseInt(port)));
                }
            }
        } catch (Exception e) {
            throw EtcdExceptionFactory.toEtcdException(e);
        }

        return addresses;
    }
}
