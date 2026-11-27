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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.maintenance.StatusResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class MaintenanceTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(3)
        .build();

    private static Client client;
    private static Maintenance maintenance;
    private static List<String> endpoints;

    @TempDir
    static Path tempDir;

    @BeforeEach
    public void setUp() {
        endpoints = cluster.clientEndpoints().stream().map(URI::toString).collect(Collectors.toList());
        client = TestUtil.client(cluster).build();
        maintenance = client.getMaintenanceClient();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testStatusMember() throws ExecutionException, InterruptedException {
        List<Long> memberIds = new ArrayList<>();
        for (String endpoint : endpoints) {
            try (Client member = Client.builder(endpoint).build()) {
                long expectedId = member.getClusterClient().listMember().get().getHeader().getMemberId();
                StatusResponse statusResponse = maintenance.statusMember(endpoint).get();
                assertThat(statusResponse.getHeader().getMemberId()).isEqualTo(expectedId);
                assertThat(statusResponse.getDbSize()).isGreaterThan(0);
                assertThat(statusResponse.getRaftIndex()).isGreaterThan(0);
                assertThat(statusResponse.getRaftAppliedIndex())
                    .isGreaterThan(0)
                    .isLessThanOrEqualTo(statusResponse.getRaftIndex());
                assertThat(statusResponse.getDbSizeInUse())
                    .isGreaterThan(0)
                    .isLessThanOrEqualTo(statusResponse.getDbSize());
                assertThat(statusResponse.isLearner()).isFalse();
                assertThat(statusResponse.getErrorList()).isEmpty();
                memberIds.add(expectedId);
            }
        }
        assertThat(memberIds).doesNotHaveDuplicates();
    }

    @Test
    public void testSnapshotToOutputStream() throws ExecutionException, InterruptedException, IOException {
        // create a snapshot file current folder.
        final Path snapfile = tempDir.resolve("snap");

        // leverage try-with-resources
        try (OutputStream stream = Files.newOutputStream(snapfile)) {
            Long bytes = maintenance.snapshot(stream).get();

            stream.flush();

            Long fsize = Files.size(snapfile);

            assertThat(bytes).isEqualTo(fsize);
        }
    }

    @Test
    public void testSnapshotChunks() throws ExecutionException, InterruptedException, TimeoutException {
        final var captured = new ByteArrayOutputStream();
        final var chunkSum = new AtomicLong();
        final var completed = new CompletableFuture<Void>();

        maintenance.snapshot(
            Maintenance.listener(
                value -> {
                    var blob = value.getBlob();
                    captured.writeBytes(blob.toByteArray());
                    chunkSum.addAndGet(blob.size());
                },
                completed::completeExceptionally,
                () -> completed.complete(null)));

        completed.get(10, TimeUnit.SECONDS);
        assertThat(chunkSum.get()).isPositive().isEqualTo(captured.size());
    }

    @Test
    public void testHashKV() throws ExecutionException, InterruptedException {
        maintenance.hashKV(endpoints.get(0), 0).get();
    }

    // TODO trigger alarm, valid whether listAlarms will work.
    // TODO disarm the alarm member, valid whether disarm will work with listAlarms.
    @Test
    public void testAlarmList() throws ExecutionException, InterruptedException {
        maintenance.listAlarms().get();
    }

    @Test
    public void testDefragment() throws ExecutionException, InterruptedException {
        maintenance.defragmentMember(endpoints.get(0)).get();
    }

    @Test
    public void testMoveLeader() throws ExecutionException, InterruptedException {
        String leaderEndpoint = null;
        List<Long> followers = new ArrayList<>();
        for (String ep : endpoints) {
            StatusResponse statusResponse = maintenance.statusMember(ep).get();
            long memberId = statusResponse.getHeader().getMemberId();
            if (memberId == statusResponse.getLeader()) {
                leaderEndpoint = ep;
                continue;
            }
            followers.add(memberId);
        }
        if (leaderEndpoint == null) {
            fail("leader not found");
        }

        try (Client client = Client.builder(leaderEndpoint.toString()).build()) {
            client.getMaintenanceClient().moveLeader(followers.get(0)).get();
        }
    }
}
