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

import java.util.stream.Collectors;

import io.etcd.jetcd.spi.EndpointTargetResolver;

import com.google.common.collect.Iterables;

public class EndpointTargetResolvers {

    public static final EndpointTargetResolver IP = (authority, endpoints) -> String.format(
        "%s://%s/%s",
        IPNameResolver.SCHEME,
        authority != null ? authority : "",
        endpoints.stream().map(e -> e.getHost() + ":" + e.getPort()).collect(Collectors.joining(",")));

    public static final EndpointTargetResolver DNS_SRV = (authority, endpoints) -> {
        if (endpoints.size() != 1) {
            throw new IllegalArgumentException("When configured for discovery, there should be only a single endpoint");
        }

        return String.format(
            "%s:///%s",
            DnsSrvNameResolver.SCHEME,
            Iterables.get(endpoints, 0));
    };

    private EndpointTargetResolvers() {
    }
}
