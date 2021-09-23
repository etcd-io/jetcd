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

package io.etcd.jetcd;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.election.CampaignResponse;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.election.NoLeaderException;
import io.etcd.jetcd.election.NotLeaderException;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static io.etcd.jetcd.TestUtil.randomByteSequence;
import static io.etcd.jetcd.TestUtil.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ElectionTest {
    private static final int OPERATION_TIMEOUT = 5;

    @RegisterExtension
    public static EtcdClusterExtension cluster = new EtcdClusterExtension("etcd-election", 3, false);
    private static Election electionClient;
    private static Lease leaseClient;
    private static KV kvClient;

    @BeforeAll
    public static void setUp() {
        Client client = Client.builder().endpoints(cluster.getClientEndpoints()).build();
        electionClient = client.getElectionClient();
        leaseClient = client.getLeaseClient();
        kvClient = client.getKVClient();
    }

    @Test
    public void testIsolatedElection() throws Exception {
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);

        // register lease
        long leaseId = leaseClient.grant(10).get().getID();

        // start new campaign
        ByteSequence firstProposal = ByteSequence.from("proposal1", StandardCharsets.UTF_8);
        CampaignResponse campaignResponse = electionClient.campaign(electionName, leaseId, firstProposal)
            .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        assertThat(campaignResponse.getLeader()).isNotNull();
        assertThat(campaignResponse.getLeader().getLease()).isEqualTo(leaseId);
        assertThat(campaignResponse.getLeader().getName()).isEqualTo(electionName.getByteString());
        // election is backed by standard key in etcd. let us examine it
        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
        List<KeyValue> keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).getKey().getByteString()).isEqualTo(campaignResponse.getLeader().getKey());
        assertThat(keys.get(0).getValue()).isEqualTo(firstProposal);

        // check that we really are the leader (just to test API)
        LeaderResponse leaderResponse = electionClient.leader(electionName)
            .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        assertThat(leaderResponse.getKv().getKey().getByteString()).isEqualTo(campaignResponse.getLeader().getKey());
        assertThat(leaderResponse.getKv().getValue()).isEqualTo(firstProposal);
        assertThat(leaderResponse.getKv().getLease()).isEqualTo(leaseId);
        assertThat(leaderResponse.getKv().getCreateRevision()).isEqualTo(campaignResponse.getLeader().getRevision());

        // as a leader change your proposal
        ByteSequence secondProposal = ByteSequence.from("proposal2", StandardCharsets.UTF_8);
        electionClient.proclaim(campaignResponse.getLeader(), secondProposal).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).getValue()).isEqualTo(secondProposal);

        // finally resign
        electionClient.resign(campaignResponse.getLeader()).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys).isEmpty();

        leaseClient.revoke(leaseId).get();
    }

    @Test
    public void testEmptyElection() throws Exception {
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
        try {
            electionClient.leader(electionName).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            fail("etcd communicates missing leader with error");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(NoLeaderException.class);
        }
    }

    @Test
    public void testRetryCampaignWithDifferentValue() throws Exception {
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
        long leaseId = leaseClient.grant(10).get().getID();

        ByteSequence firstProposal = ByteSequence.from("proposal1", StandardCharsets.UTF_8);
        CampaignResponse campaignResponse1 = electionClient.campaign(electionName, leaseId, firstProposal)
            .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);

        ByteSequence secondProposal = ByteSequence.from("proposal2", StandardCharsets.UTF_8);
        CampaignResponse campaignResponse2 = electionClient.campaign(electionName, leaseId, secondProposal)
            .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);

        // check that for sure we are the leader
        LeaderResponse leaderResponse = electionClient.leader(electionName).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        assertThat(leaderResponse.getKv().getKey().getByteString()).isEqualTo(campaignResponse1.getLeader().getKey());
        assertThat(campaignResponse1.getLeader().getKey()).isEqualTo(campaignResponse2.getLeader().getKey());
        assertThat(campaignResponse1.getLeader().getRevision()).isEqualTo(campaignResponse2.getLeader().getRevision());

        // latest proposal should be persisted
        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
        List<KeyValue> keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).getValue()).isEqualTo(secondProposal);

        electionClient.resign(campaignResponse1.getLeader()).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        leaseClient.revoke(leaseId).get();
    }

    @Test
    public void testProposeValueNotBeingLeader() throws Exception {
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
        LeaderKey leaderKey = new LeaderKey(electionName.getByteString(), randomByteSequence().getByteString(), 1, 1);
        ByteSequence proposal = ByteSequence.from("proposal", StandardCharsets.UTF_8);
        try {
            electionClient.proclaim(leaderKey, proposal).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            fail("Cannot proclaim proposal not being a leader");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(NotLeaderException.class);
        }
        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
        List<KeyValue> keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys).isEmpty();
    }

    @Test
    public void testObserveElections() throws Exception {
        int electionCount = 3;
        final AtomicInteger electionsSeen = new AtomicInteger(0);
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);

        electionClient.observe(electionName, new Election.Listener() {
            @Override
            public void onNext(LeaderResponse response) {
                electionsSeen.incrementAndGet();
            }

            @Override
            public void onError(Throwable error) {
            }

            @Override
            public void onCompleted() {
            }
        });

        long leaseId = leaseClient.grant(10).get().getID();

        for (int i = 0; i < electionCount; ++i) {
            ByteSequence proposal = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
            CampaignResponse campaignResponse = electionClient.campaign(electionName, leaseId, proposal)
                .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            Thread.sleep(100);
            electionClient.resign(campaignResponse.getLeader()).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        }

        TestUtil.waitForCondition(
            () -> electionsSeen.get() == electionCount, OPERATION_TIMEOUT * 1000,
            "Observer did not receive expected notifications, got: " + electionsSeen.get());

        leaseClient.revoke(leaseId).get();
    }

    @Test
    public void testSynchronizationBarrier() throws Exception {
        final int threadCount = 5;
        final Random random = new Random();
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
        final AtomicInteger sharedVariable = new AtomicInteger(0);
        // create separate clients so they will compete for access to shared resource
        List<Client> clients = new ArrayList<>(threadCount);
        List<Long> leases = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; ++i) {
            Client client = Client.builder().endpoints(cluster.getClientEndpoints()).build();
            long leaseId = client.getLeaseClient().grant(100).get().getID();
            clients.add(client);
            leases.add(leaseId);
        }
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; ++i) {
            final int id = i;
            final ByteSequence proposal = ByteSequence.from(Integer.toString(id), StandardCharsets.UTF_8);
            futures.add(executor.submit(() -> {
                try {
                    Election electionClient = clients.get(id).getElectionClient();
                    CampaignResponse campaignResponse = electionClient.campaign(electionName, leases.get(id), proposal)
                        .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
                    int localCopy = sharedVariable.get();
                    Thread.sleep(200 + random.nextInt(300));
                    sharedVariable.set(localCopy + 1);
                    electionClient.resign(campaignResponse.getLeader()).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    fail("Unexpected error in thread {}: {}", id, e);
                }
            }));
        }
        executor.shutdown();
        executor.awaitTermination(threadCount * OPERATION_TIMEOUT, TimeUnit.SECONDS);
        futures.forEach(f -> assertThat(f).isDone());
        assertThat(sharedVariable.get()).isEqualTo(threadCount);
        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
        assertThat(kvClient.get(electionName, getOption).get().getCount()).isEqualTo(0L);
        for (int i = 0; i < threadCount; ++i) {
            clients.get(i).getLeaseClient().revoke(leases.get(i)).get();
            clients.get(i).close();
        }
    }
}
