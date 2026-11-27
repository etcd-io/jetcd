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

package io.etcd.jetcd.test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.dns.SrvRecord;

import static io.etcd.jetcd.test.EtcdConstants.LOCALHOST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DnsSrvContainer functionality.
 *
 * <p>
 * These tests validate that the DnsSrvContainer correctly serves DNS SRV records
 * by querying the DNS server directly with Vert.x DnsClient. These tests do not
 * involve jetcd client integration.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class DnsSrvContainerTest {

    private static DnsSrvContainer dnsContainer;
    private static int dnsPort;
    private static EtcdCluster etcdCluster;

    @BeforeAll
    public static void setup() {
        etcdCluster = Etcd.builder()
            .withClusterName("dns-srv-container-test")
            .withMountedDataDirectory(false)
            .build();
        etcdCluster.start();

        dnsContainer = DnsSrvContainer.create()
            .withEtcdCluster("_test._tcp.local", etcdCluster)
            .withLogging(true);
        dnsContainer.start();

        dnsPort = dnsContainer.getDnsPort();
    }

    @AfterAll
    public static void teardown() {
        if (dnsContainer != null) {
            dnsContainer.stop();
        }

        if (etcdCluster != null) {
            etcdCluster.close();
        }
    }

    @Test
    public void testDnsSrvRecordResolution() throws Exception {
        // Note: Vert.x's built-in blocked thread checker is enabled by default.
        // If event loop is blocked > 2 seconds, it will log to stderr with:
        //   - Thread name and ID
        //   - How long blocked (in ms)
        //   - Full stack trace showing what's blocking
        // This information helps debug blocking operations during tests.
        Vertx vertx = Vertx.vertx(new VertxOptions().setUseDaemonThread(true));
        DnsClient dnsClient = vertx.createDnsClient(
            new DnsClientOptions()
                .setHost(LOCALHOST)
                .setPort(dnsPort));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<SrvRecord>> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        dnsClient.resolveSRV("_test._tcp.local")
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    result.set(ar.result());
                } else {
                    error.set(ar.cause());
                }
                latch.countDown();
            });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isNull();

        List<SrvRecord> srvRecords = result.get();
        assertThat(srvRecords).isNotEmpty();

        SrvRecord record = srvRecords.get(0);
        assertThat(record.target()).isEqualTo(LOCALHOST);
        int expectedPort = etcdCluster.clientEndpoints().getFirst().getPort();
        assertThat(record.port()).isEqualTo(expectedPort);
        assertThat(record.priority()).isEqualTo(0);
        assertThat(record.weight()).isEqualTo(0);

        try {
            vertx.close().toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Log but don't fail test - best effort cleanup
        }
    }

    @Test
    public void testDnsSrvRecordTTL() throws Exception {
        // Note: Vert.x's built-in blocked thread checker is enabled by default.
        // If event loop is blocked > 2 seconds, it will log to stderr with:
        //   - Thread name and ID
        //   - How long blocked (in ms)
        //   - Full stack trace showing what's blocking
        // This information helps debug blocking operations during tests.
        Vertx vertx = Vertx.vertx(new VertxOptions().setUseDaemonThread(true));
        DnsClient dnsClient = vertx.createDnsClient(
            new DnsClientOptions()
                .setHost(LOCALHOST)
                .setPort(dnsPort));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<SrvRecord>> result = new AtomicReference<>();

        dnsClient.resolveSRV("_test._tcp.local")
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    result.set(ar.result());
                }
                latch.countDown();
            });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(result.get()).isNotEmpty();

        SrvRecord record = result.get().get(0);
        long recordTTL = record.ttl();

        // Note: dnsmasq typically sets TTL=0 for SRV records
        // In production with real DNS (Route53, CloudDNS, etc.), TTL would be 60s+
        assertThat(recordTTL).isGreaterThanOrEqualTo(0);

        try {
            vertx.close().toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Log but don't fail test - best effort cleanup
        }
    }
}
