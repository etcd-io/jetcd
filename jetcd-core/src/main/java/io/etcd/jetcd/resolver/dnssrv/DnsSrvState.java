/*
 * Copyright 2016-2025 The jetcd authors
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

package io.etcd.jetcd.resolver.dnssrv;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.SrvRecord;
import io.vertx.core.spi.endpoint.EndpointBuilder;

/**
 * Manages the lifecycle of DNS SRV resolution including caching and TTL-based refresh.
 * Thread-safe state management for resolved endpoints.
 *
 * @param <B> the endpoint builder type
 */
final class DnsSrvState<B> {
    private final Vertx vertx;
    private final DnsClient client;
    private final String serviceName;
    private final int minTTL;
    private final EndpointBuilder<B, SrvRecord> builder;
    private B endpoints;
    private Long timerId;
    private boolean disposed;
    private Future<DnsSrvState<B>> inflightRefresh;

    private DnsSrvState(
        Vertx vertx,
        DnsSrvClientOptions options,
        EndpointBuilder<B, SrvRecord> builder) {
        this.vertx = vertx;
        this.client = vertx.createDnsClient(options.getDnsOptions());
        this.serviceName = options.getServiceName();
        this.minTTL = options.getMinTTL();
        this.builder = builder;
    }

    /**
     * Factory method that creates a new state and performs initial DNS resolution.
     *
     * @param  vertx   the Vertx instance
     * @param  options the DNS SRV client options
     * @param  builder the endpoint builder
     * @return         a Future that completes with the initialized state
     */
    static <B> Future<DnsSrvState<B>> create(
        Vertx vertx,
        DnsSrvClientOptions options,
        EndpointBuilder<B, SrvRecord> builder) {
        return new DnsSrvState<>(vertx, options, builder).refresh();
    }

    /**
     * Performs async DNS SRV resolution and caches the results.
     * Schedules automatic refresh based on TTL.
     *
     * @return a Future that completes with this state instance
     */
    synchronized Future<DnsSrvState<B>> refresh() {
        if (disposed) {
            return Future.succeededFuture(this);
        }

        // Return existing in-flight refresh if one exists
        if (inflightRefresh != null) {
            return inflightRefresh;
        }

        inflightRefresh = client
            .resolveSRV(serviceName)
            .map(this::buildEndpoints)
            .map(this::updateStateAndScheduleRefresh)
            .onComplete(ar -> {
                synchronized (this) {
                    inflightRefresh = null;
                }
            });

        return inflightRefresh;
    }

    private EndpointsWithTTL<B> buildEndpoints(java.util.List<SrvRecord> records) {
        EndpointBuilder<B, SrvRecord> tmp = builder;
        long ttl = Long.MAX_VALUE;

        for (SrvRecord record : records) {
            tmp = tmp.addServer(record, record.target() + "-" + record.port());
            ttl = Math.min(ttl, record.ttl());
        }

        return new EndpointsWithTTL<>(tmp.build(), ttl);
    }

    private synchronized DnsSrvState<B> updateStateAndScheduleRefresh(EndpointsWithTTL<B> result) {
        if (disposed) {
            return this;
        }

        endpoints = result.endpoints;

        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }

        long ttl = Math.max(result.ttl, minTTL);

        if (ttl > 0 && ttl != Long.MAX_VALUE) {
            timerId = vertx.setTimer(ttl * 1000, id -> refresh());
        }

        return this;
    }

    private record EndpointsWithTTL<B>(B endpoints, long ttl) {
    }

    synchronized B endpoints() {
        return endpoints;
    }

    synchronized boolean isValid() {
        return !disposed;
    }

    synchronized void dispose() {
        disposed = true;
        if (timerId != null) {
            vertx.cancelTimer(timerId);
            timerId = null;
        }
        client.close();
    }
}
