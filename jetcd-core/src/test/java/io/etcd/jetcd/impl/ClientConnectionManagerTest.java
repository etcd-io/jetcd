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

package io.etcd.jetcd.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.resolver.ServiceResolver;
import io.etcd.jetcd.resolver.ServiceResolvers;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static io.etcd.jetcd.test.EtcdConstants.GOOGLE_DNS;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class ClientConnectionManagerTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    @Test
    public void testEndpoints() throws InterruptedException, ExecutionException, TimeoutException {
        try (Client client = Client.builder(cluster.clientEndpoints()).build()) {
            client.getKVClient().put(bytesOf("sample_key"), bytesOf("sample_key")).get(15, TimeUnit.SECONDS);
        }
    }

    /**
     * Tests that the DNS SRV resolver API exists and can be created.
     * This verifies the API surface but does not test actual DNS resolution
     * (which would require DNS infrastructure setup).
     *
     * For real DNS SRV usage examples, see docs/DNS_SRV_RESOLUTION.md
     */
    @Test
    public void testDnsSrvResolverCreation() {
        // Verify DNS SRV resolver can be created with service name only
        ServiceResolver resolver1 = ServiceResolvers.dnsSrv("_etcd._tcp.example.com");
        assertThat(resolver1).isNotNull();
        assertThat(resolver1.getTarget()).isNotNull();
        assertThat(resolver1.getResolver()).isNotNull();

        // Verify DNS SRV resolver can be created with custom DNS server
        ServiceResolver resolver2 = ServiceResolvers.dnsSrv(
            "_etcd._tcp.example.com",
            GOOGLE_DNS,
            53);
        assertThat(resolver2).isNotNull();
        assertThat(resolver2.getTarget()).isNotNull();
        assertThat(resolver2.getResolver()).isNotNull();
    }
}
