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

package io.etcd.jetcd.resolver.dnssrv;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.resolver.ServiceResolvers;
import io.etcd.jetcd.test.DnsSrvContainer;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static io.etcd.jetcd.test.EtcdConstants.LOCALHOST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for jetcd DNS SRV resolver.
 *
 * <p>
 * Tests that jetcd Client can connect to etcd using DNS SRV records
 * and perform operations correctly. These tests validate the full integration
 * between jetcd's DNS SRV resolver and actual etcd instances.
 *
 * <p>
 * For tests that validate DnsSrvContainer functionality (without jetcd),
 * see {@link io.etcd.jetcd.test.DnsSrvContainerTest}.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class DnsSrvTest {

    private static DnsSrvContainer dnsContainer;
    private static EtcdCluster singleNodeCluster;

    @BeforeAll
    public static void setupDnsmasq() {
        singleNodeCluster = Etcd.builder()
            .withNodes(1)
            .withClusterName("test-single")
            .withMountedDataDirectory(false)
            .build();

        singleNodeCluster.start();

        dnsContainer = DnsSrvContainer.create()
            .withEtcdCluster("_etcd._tcp.single.test.local", singleNodeCluster)
            .withLogging(true);

        dnsContainer.start();
    }

    @AfterAll
    public static void teardown() {
        if (dnsContainer != null) {
            dnsContainer.stop();
        }

        if (singleNodeCluster != null) {
            singleNodeCluster.close();
        }
    }

    @Test
    public void testJetcdClientWithDnsSrv() throws Exception {
        var resolver = ServiceResolvers.dnsSrv(
            "_etcd._tcp.single.test.local",
            LOCALHOST,
            dnsContainer.getDnsPort());

        try (Client client = Client.builder(resolver).build()) {
            KV kv = client.getKVClient();

            ByteSequence key = bytesOf("dns_srv_single_test");
            ByteSequence value = bytesOf("test_value");

            kv.put(key, value).get(10, TimeUnit.SECONDS);

            GetResponse getResp = kv.get(key).get(10, TimeUnit.SECONDS);
            assertThat(getResp.getCount()).isEqualTo(1);
            assertThat(getResp.getKvs().get(0).getValue()).isEqualTo(value);

            kv.delete(key).get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testJetcdDnsSrvResolverLifecycle() throws Exception {
        // Verifies that jetcd client works with DNS SRV resolver
        // The resolver handles TTL-based refresh internally
        var resolver = ServiceResolvers.dnsSrv(
            "_etcd._tcp.single.test.local",
            LOCALHOST,
            dnsContainer.getDnsPort());

        try (Client client = Client.builder(resolver).build()) {
            KV kv = client.getKVClient();
            ByteSequence key = bytesOf("resolver_lifecycle_test");
            ByteSequence value = bytesOf("test_value");

            kv.put(key, value).get(10, TimeUnit.SECONDS);

            GetResponse getResp = kv.get(key).get(10, TimeUnit.SECONDS);
            assertThat(getResp.getCount()).isEqualTo(1);
            assertThat(getResp.getKvs().get(0).getValue()).isEqualTo(value);

            kv.delete(key).get(10, TimeUnit.SECONDS);
        }
    }

}
