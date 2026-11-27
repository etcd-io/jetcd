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
import io.vertx.core.dns.SrvRecord;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.core.spi.endpoint.EndpointResolver;

/**
 * Fully async DNS SRV endpoint resolver that performs lazy on-demand resolution.
 * No blocking calls - all DNS queries are async using Vert.x Future chains.
 * Supports TTL-based automatic refresh of DNS records.
 *
 * @param <B> the endpoint builder type
 */
record DnsSrvEndpointResolver<B>(
    Vertx vertx,
    DnsSrvClientOptions options) implements EndpointResolver<SocketAddress, SrvRecord, DnsSrvState<B>, B> {

    @Override
    public SocketAddress tryCast(Address address) {
        return address instanceof SocketAddress socketAddress ? socketAddress : null;
    }

    @Override
    public Future<DnsSrvState<B>> resolve(SocketAddress address, EndpointBuilder<B, SrvRecord> builder) {
        return DnsSrvState.create(vertx, options, builder);
    }

    @Override
    public B endpoint(DnsSrvState<B> state) {
        return state.endpoints();
    }

    @Override
    public SocketAddress addressOf(SrvRecord record) {
        return SocketAddress.inetSocketAddress(record.port(), record.target());
    }

    @Override
    public boolean isValid(DnsSrvState<B> state) {
        return state.isValid();
    }

    @Override
    public void dispose(DnsSrvState<B> state) {
        state.dispose();
    }

    @Override
    public void close() {
        // DnsClient cleanup handled by Vert.x
    }
}
