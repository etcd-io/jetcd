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

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.restassured.RestAssured;

import com.google.common.base.Charsets;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 45, unit = TimeUnit.SECONDS)
public class LeaseMemoryLeakTest {

    @RegisterExtension
    public static final EtcdClusterExtension CLUSTER = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    private Client client;
    private KV kvClient;
    private Lease leaseClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaseMemoryLeakTest.class);
    private static final long TTL = 10;
    private static final int ITERATIONS = 5;
    private static final Pattern GO_ROUTINES_EXTRACT_PATTERN = Pattern.compile("go_goroutines (\\d+)");
    private static final ByteSequence KEY = ByteSequence.from("foo", Charsets.UTF_8);
    private static final ByteSequence VALUE = ByteSequence.from("bar", Charsets.UTF_8);

    @BeforeEach
    public void setUp() {
        client = TestUtil.client(CLUSTER).build();
        kvClient = client.getKVClient();
        leaseClient = client.getLeaseClient();
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // https://github.com/etcd-io/jetcd/issues/1236
    @Test
    public void testKeepAliveOnceMemoryLeak() throws Exception {
        final long leaseID = leaseClient.grant(TTL).get(1, TimeUnit.SECONDS).getID();
        final URI uri = CLUSTER.cluster().clientEndpoints().get(0);
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            LOGGER.info("Put K/V with lease id {}", leaseID);
            kvClient.put(KEY, VALUE, PutOption.builder().withLeaseId(leaseID).build()).get(1, TimeUnit.SECONDS);

            AtomicInteger max = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(ITERATIONS);

            int start = extractGoRoutinesCount(uri);
            assertThat(start).isGreaterThan(0);

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (latch.getCount() == 0) {
                        return;
                    }

                    LOGGER.info("Keep alive {} once for lease id {}", ITERATIONS - latch.getCount(), leaseID);
                    leaseClient.keepAliveOnce(leaseID).get(5, TimeUnit.SECONDS);

                    int count = extractGoRoutinesCount(uri);
                    max.set(Math.max(max.get(), count));

                    LOGGER.info("go-routines:{}, max: {}, start: {}", count, max.get(), start);

                    latch.countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, 0, 1, TimeUnit.SECONDS);

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(max.get()).isGreaterThan(0);
            assertThat(max.get()).isGreaterThanOrEqualTo(start);

            if (max.get() != start) {
                assertThat(max.get() - start).isCloseTo(5, Percentage.withPercentage(80));
            }
        } finally {
            scheduler.shutdownNow();
        }
    }

    private int extractGoRoutinesCount(URI uri) {
        var metrics = RestAssured.given()
            .baseUri(uri.toString())
            .when()
            .get("/metrics")
            .then()
            .statusCode(200)
            .extract().asString();

        final Matcher matcher = GO_ROUTINES_EXTRACT_PATTERN.matcher(metrics);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return -1;
    }
}
