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

package io.etcd.jetcd.resolver.dnssrv;

import java.net.URI;
import java.util.Collections;

import io.etcd.jetcd.resolver.DirectUriResolver;
import io.etcd.jetcd.resolver.SmartNameResolver;
import io.etcd.jetcd.resolver.URIResolverLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NameResolverTest {
    private static final String[] DNSSRV_SCHEMES = new String[] { "dns+srv", "dnssrv", "srv" };

    @Test
    public void testUriResolverDiscovery() {
        final URIResolverLoader loader = URIResolverLoader.defaultLoader();
        final SmartNameResolver resolver = new SmartNameResolver("etcd", Collections.emptyList(), loader);

        assertThat(resolver.getResolvers().stream().anyMatch(DnsSrvUriResolver.class::isInstance)).isTrue();
        assertThat(resolver.getResolvers().stream().anyMatch(DirectUriResolver.class::isInstance)).isTrue();
    }

    @Test
    public void testDnsSrvResolver() {
        final DnsSrvUriResolver discovery = new DnsSrvUriResolver();

        for (String scheme : DNSSRV_SCHEMES) {
            URI uri = URI.create(scheme + "://_xmpp-server._tcp.gmail.com");

            assertThat(discovery.supports(uri)).isTrue();
            assertThat(discovery.resolve(uri).size()).isGreaterThan(0);
        }
    }
}
