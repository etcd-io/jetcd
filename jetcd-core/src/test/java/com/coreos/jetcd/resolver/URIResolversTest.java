/**
 * Copyright 2017 The jetcd authors
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
package com.coreos.jetcd.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.coreos.jetcd.exception.EtcdException;
import java.net.URI;
import org.junit.Test;

public class URIResolversTest {
  private static final String[] DIRECT_SCHEMES = new String[] { "http", "https" };
  private static final String[] DNSSRV_SCHEMES = new String[] { "dns+srv", "dnssrv", "srv" };

  @Test
  public void testDirectResolver() throws Exception {
    final URIResolvers.Direct discovery = new URIResolvers.Direct();

    for (String scheme : DIRECT_SCHEMES) {
      URI uri = URI.create(scheme + "://127.0.0.1:2379");

      assertThat(discovery.supports(uri)).isTrue();
      assertThat(discovery.resolve(uri).size()).isGreaterThan(0);
    }
  }

  @Test
  public void testUnsupportedDirectSchema() throws Exception {
    final URIResolvers.Direct discovery = new URIResolvers.Direct();
    final URI uri = URI.create("mailto://127.0.0.1:2379");

    assertThat(discovery.supports(uri)).isFalse();
    assertThatExceptionOfType(EtcdException.class)
        .isThrownBy(() -> discovery.resolve(uri))
        .withMessageContaining("Unsupported URI " + uri);
  }

  @Test
  public void testDnsSrvResolver() throws Exception {
    final URIResolvers.DnsSrv discovery = new URIResolvers.DnsSrv();

    for (String scheme : DNSSRV_SCHEMES) {
      URI uri = URI.create(scheme + "://_xmpp-server._tcp.gmail.com");

      assertThat(discovery.supports(uri)).isTrue();
      assertThat(discovery.resolve(uri).size()).isGreaterThan(0);
    }
  }
}
