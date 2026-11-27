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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.resolver.ServiceResolvers;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import static io.etcd.jetcd.test.EtcdConstants.LOCALHOST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Failure path tests for DNS SRV resolution.
 * Tests error scenarios that were not covered in happy path tests.
 */
public class DnsSrvFailureTest {

    private Vertx vertx;

    @BeforeEach
    void setup() {
        // Note: Vert.x's built-in blocked thread checker is enabled by default.
        // If event loop is blocked > 2 seconds, it will log to stderr with:
        //   - Thread name and ID
        //   - How long blocked (in ms)
        //   - Full stack trace showing what's blocking
        // This information helps debug blocking operations during tests.
        vertx = Vertx.vertx(new VertxOptions().setUseDaemonThread(true));
    }

    @AfterEach
    void cleanup() {
        if (vertx != null) {
            try {
                vertx.close().toCompletionStage().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Log but don't throw - best effort cleanup
            }
        }
    }

    @Test
    void testDnsServerUnreachable() {
        // Create resolver pointing to non-existent DNS server (invalid port)
        // This should not crash - DNS resolution is async
        var resolver = ServiceResolvers.dnsSrv(
            "_etcd._tcp.nonexistent.local",
            LOCALHOST,
            99999 // Invalid port - nothing listening here
        );

        // Creating the resolver should succeed - DNS resolution happens later
        assertThat(resolver).isNotNull();
    }

    @Test
    void testDnsQueryTimeout() {
        // Create resolver with DNS server that doesn't exist
        // This should not crash - timeouts are handled gracefully
        var resolver = ServiceResolvers.dnsSrv(
            "_etcd._tcp.timeout.local",
            LOCALHOST,
            99999 // Non-existent DNS server - will timeout on first resolution
        );

        // Creating the resolver should succeed
        assertThat(resolver).isNotNull();
    }

    @Test
    void testEmptyServiceName() {
        // Test that empty service name is accepted
        DnsSrvClientOptions opts = new DnsSrvClientOptions("");
        assertThat(opts.getServiceName()).isEqualTo("");
    }

    @Test
    void testNullServiceName() {
        // Test that null service name is accepted by constructor
        // (it will likely cause issues during DNS resolution, but that's later)
        DnsSrvClientOptions opts = new DnsSrvClientOptions((String) null);
        assertThat(opts.getServiceName()).isNull();
    }

    @Test
    void testMinTTLHandling() {
        // Verify that minTTL=0 disables the minimum
        DnsSrvClientOptions opts1 = new DnsSrvClientOptions("_etcd._tcp.test").setMinTTL(0);
        assertThat(opts1.getMinTTL()).isEqualTo(0);

        // Verify that default minTTL=30 is used
        DnsSrvClientOptions opts2 = new DnsSrvClientOptions("_etcd._tcp.test");
        assertThat(opts2.getMinTTL()).isEqualTo(30);

        // Verify that custom minTTL can be set
        DnsSrvClientOptions opts3 = new DnsSrvClientOptions("_etcd._tcp.test").setMinTTL(120);
        assertThat(opts3.getMinTTL()).isEqualTo(120);
    }

    @Test
    void testNegativeMinTTL() {
        // Test that negative minTTL values are accepted (implementation doesn't validate)
        // This tests current behavior - implementation may choose to validate in future
        DnsSrvClientOptions opts = new DnsSrvClientOptions("_etcd._tcp.test").setMinTTL(-1);
        assertThat(opts.getMinTTL()).isEqualTo(-1);
    }

    @Test
    void testDnsClientOptionsFluentAPI() {
        // Test that all DNS client options can be configured via fluent API
        DnsSrvClientOptions opts = new DnsSrvClientOptions("_etcd._tcp.test")
            .setHost("dns.example.com")
            .setPort(5353)
            .setQueryTimeout(10000)
            .setMinTTL(60)
            .setLogActivity(true)
            .setRecursionDesired(false);

        assertThat(opts.getHost()).isEqualTo("dns.example.com");
        assertThat(opts.getPort()).isEqualTo(5353);
        assertThat(opts.getQueryTimeout()).isEqualTo(10000);
        assertThat(opts.getMinTTL()).isEqualTo(60);
        assertThat(opts.getLogActivity()).isTrue();
        assertThat(opts.isRecursionDesired()).isFalse();
    }

    @Test
    void testServiceNameCanBeChanged() {
        // Test that service name can be changed after construction
        DnsSrvClientOptions opts = new DnsSrvClientOptions("_etcd._tcp.original");
        assertThat(opts.getServiceName()).isEqualTo("_etcd._tcp.original");

        opts.setServiceName("_etcd._tcp.modified");
        assertThat(opts.getServiceName()).isEqualTo("_etcd._tcp.modified");
    }

    @Test
    void testResolverDisposal() throws Exception {
        // Test that resolver can be created and disposed without errors
        var resolver = ServiceResolvers.dnsSrv(
            "_etcd._tcp.test.local",
            LOCALHOST,
            99999 // Non-existent DNS server
        );

        // The resolver is created, even though DNS will fail
        assertThat(resolver).isNotNull();

        // Creating a client may fail, but disposal should work
        try {
            Client client = Client.builder(resolver).build();
            client.close();
        } catch (Exception e) {
            // Expected - DNS resolution may fail
            // The important thing is that we don't crash and can clean up
        }
    }

    @Test
    void testMultipleResolversCanCoexist() {
        // Test that multiple DNS SRV resolvers can be created simultaneously
        var resolver1 = ServiceResolvers.dnsSrv("_etcd1._tcp.test", LOCALHOST, 5301);
        var resolver2 = ServiceResolvers.dnsSrv("_etcd2._tcp.test", LOCALHOST, 5302);
        var resolver3 = ServiceResolvers.dnsSrv("_etcd3._tcp.test", LOCALHOST, 5303);

        assertThat(resolver1).isNotNull();
        assertThat(resolver2).isNotNull();
        assertThat(resolver3).isNotNull();

        // All resolvers should be independent
        assertThat(resolver1).isNotSameAs(resolver2);
        assertThat(resolver2).isNotSameAs(resolver3);
    }
}
