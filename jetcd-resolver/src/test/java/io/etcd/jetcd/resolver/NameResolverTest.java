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

import java.net.URI;
import java.util.Collections;

import io.etcd.jetcd.common.exception.EtcdException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class NameResolverTest {
    private static final String[] DIRECT_SCHEMES = new String[] { "http", "https" };

    @Test
    public void testDefaults() throws Exception {
        final URIResolverLoader loader = URIResolverLoader.defaultLoader();
        final SmartNameResolver resolver = new SmartNameResolver("etcd", Collections.emptyList(), loader);

        Assertions.assertThat(resolver.getResolvers().stream().anyMatch(DirectUriResolver.class::isInstance)).isTrue();
    }

    @Test
    public void testDirectResolver() throws Exception {
        final DirectUriResolver discovery = new DirectUriResolver();

        for (String scheme : DIRECT_SCHEMES) {
            URI uri = URI.create(scheme + "://127.0.0.1:2379");

            Assertions.assertThat(discovery.supports(uri)).isTrue();
            Assertions.assertThat(discovery.resolve(uri).size()).isGreaterThan(0);
        }
    }

    @Test
    public void testUnsupportedDirectSchema() throws Exception {
        final DirectUriResolver discovery = new DirectUriResolver();
        final URI uri = URI.create("mailto://127.0.0.1:2379");

        Assertions.assertThat(discovery.supports(uri)).isFalse();
        Assertions.assertThatExceptionOfType(EtcdException.class).isThrownBy(() -> discovery.resolve(uri))
            .withMessageContaining("Unsupported URI " + uri);
    }
}
