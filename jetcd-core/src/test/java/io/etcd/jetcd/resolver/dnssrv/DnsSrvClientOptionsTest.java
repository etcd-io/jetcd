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

import org.junit.jupiter.api.Test;

import io.netty.handler.logging.ByteBufFormat;
import io.vertx.core.dns.DnsClientOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DnsSrvClientOptions.
 */
public class DnsSrvClientOptionsTest {

    @Test
    void testDnsClientOptionsDefensiveCopy() {
        // Create a DnsClientOptions with specific configuration
        DnsClientOptions base = new DnsClientOptions()
            .setHost("dns1.example.com")
            .setPort(5353)
            .setQueryTimeout(10000)
            .setLogActivity(true)
            .setActivityLogFormat(ByteBufFormat.SIMPLE)
            .setRecursionDesired(true);

        // Create DnsSrvClientOptions with the base options
        DnsSrvClientOptions srvOpts = new DnsSrvClientOptions(base, "_etcd._tcp.service");

        // Modify the original - should NOT affect srvOpts due to defensive copy
        base.setHost("dns2.example.com");
        base.setPort(9999);
        base.setQueryTimeout(30000);
        base.setLogActivity(false);
        base.setActivityLogFormat(ByteBufFormat.HEX_DUMP);
        base.setRecursionDesired(false);

        // Verify srvOpts retained the original values (defensive copy worked)
        assertThat(srvOpts.getDnsOptions().getHost()).isEqualTo("dns1.example.com");
        assertThat(srvOpts.getDnsOptions().getPort()).isEqualTo(5353);
        assertThat(srvOpts.getDnsOptions().getQueryTimeout()).isEqualTo(10000);
        assertThat(srvOpts.getDnsOptions().getLogActivity()).isTrue();
        assertThat(srvOpts.getDnsOptions().getActivityLogFormat()).isEqualTo(ByteBufFormat.SIMPLE);
        assertThat(srvOpts.getDnsOptions().isRecursionDesired()).isTrue();

        // Verify the serviceName is set correctly
        assertThat(srvOpts.getServiceName()).isEqualTo("_etcd._tcp.service");
    }

    @Test
    void testNullBaseOptions() {
        // Test that null base options creates a new DnsClientOptions
        DnsSrvClientOptions srvOpts = new DnsSrvClientOptions(null, "_etcd._tcp.service");

        assertThat(srvOpts.getDnsOptions()).isNotNull();
        assertThat(srvOpts.getServiceName()).isEqualTo("_etcd._tcp.service");
    }

    @Test
    void testFluentApiDoesNotAffectOriginal() {
        DnsClientOptions base = new DnsClientOptions()
            .setHost("dns1.example.com")
            .setPort(5353);

        DnsSrvClientOptions srvOpts = new DnsSrvClientOptions(base, "_etcd._tcp.service");

        // Configure srvOpts via fluent API
        srvOpts.setHost("dns2.example.com")
            .setPort(8080)
            .setQueryTimeout(5000);

        // Original base should not be affected because of defensive copy
        assertThat(base.getHost()).isEqualTo("dns1.example.com");
        assertThat(base.getPort()).isEqualTo(5353);

        // srvOpts should have the new values
        assertThat(srvOpts.getHost()).isEqualTo("dns2.example.com");
        assertThat(srvOpts.getPort()).isEqualTo(8080);
        assertThat(srvOpts.getQueryTimeout()).isEqualTo(5000);
    }

    @Test
    void testDefaultMinTTL() {
        DnsSrvClientOptions opts = new DnsSrvClientOptions("_etcd._tcp.service");
        assertThat(opts.getMinTTL()).isEqualTo(30);
    }

    @Test
    void testMinTTLCanBeOverridden() {
        DnsSrvClientOptions opts = new DnsSrvClientOptions("_etcd._tcp.service")
            .setMinTTL(60);
        assertThat(opts.getMinTTL()).isEqualTo(60);
    }

    @Test
    void testMinTTLCanBeSetToZero() {
        DnsSrvClientOptions opts = new DnsSrvClientOptions("_etcd._tcp.service")
            .setMinTTL(0);
        assertThat(opts.getMinTTL()).isEqualTo(0);
    }
}
