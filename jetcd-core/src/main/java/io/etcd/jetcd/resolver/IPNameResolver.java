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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;

public class IPNameResolver extends AbstractNameResolver {
    public static final String SCHEME = "ip";
    public static final int ETCD_CLIENT_PORT = 2379;

    private final List<HostAndPort> addresses;

    public IPNameResolver(URI targetUri) {
        super(targetUri);

        this.addresses = Stream.of(targetUri.getPath().split(","))
            .map(address -> address.startsWith("/") ? address.substring(1) : address)
            .map(HostAndPort::fromString)
            .collect(Collectors.toList());
    }

    @Override
    protected List<EquivalentAddressGroup> computeAddressGroups() {
        if (addresses.isEmpty()) {
            throw EtcdExceptionFactory.newEtcdException(
                ErrorCode.INVALID_ARGUMENT,
                "Unable to resolve endpoint " + getTargetUri());
        }

        return addresses.stream()
            .map(address -> {
                return new EquivalentAddressGroup(
                    new InetSocketAddress(
                        address.getHost(),
                        address.getPortOrDefault(ETCD_CLIENT_PORT)),
                    Strings.isNullOrEmpty(getServiceAuthority())
                        ? Attributes.newBuilder()
                            .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, address.toString())
                            .build()
                        : Attributes.EMPTY);
            })
            .collect(Collectors.toList());
    }
}
