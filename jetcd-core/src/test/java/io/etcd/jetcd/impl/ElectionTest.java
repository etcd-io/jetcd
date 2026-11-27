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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Election;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.election.CampaignResponse;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.election.NoLeaderException;
import io.etcd.jetcd.election.NotLeaderException;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static io.etcd.jetcd.impl.TestUtil.randomByteSequence;
import static io.etcd.jetcd.impl.TestUtil.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class ElectionTest {
    private static final int OPERATION_TIMEOUT = 5;

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    private static Client client;
    private static Election electionClient;
    private static Lease leaseClient;
    private static KV kvClient;

    @BeforeAll
    public static void setUp() {
        client = TestUtil.client(cluster).build();
        electionClient = client.getElectionClient();
        leaseClient = client.getLeaseClient();
        kvClient = client.getKVClient();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
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
        assertThat(campaignResponse.getLeader().lease()).isEqualTo(leaseId);
        assertThat(campaignResponse.getLeader().name()).isEqualTo(electionName);
        // election is backed by standard key in etcd. let us examine it
        GetOption getOption = GetOption.builder().isPrefix(true).build();
        List<KeyValue> keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).getKey().toString()).isEqualTo(campaignResponse.getLeader().key().toString());
        assertThat(keys.get(0).getValue()).isEqualTo(firstProposal);

        // check that we really are the leader (just to test API)
        LeaderResponse leaderResponse = electionClient.leader(electionName)
            .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        assertThat(leaderResponse.getKv().getKey()).isEqualTo(campaignResponse.getLeader().key());
        assertThat(leaderResponse.getKv().getValue()).isEqualTo(firstProposal);
        assertThat(leaderResponse.getKv().getLease()).isEqualTo(leaseId);
        assertThat(leaderResponse.getKv().getCreateRevision()).isEqualTo(campaignResponse.getLeader().revision());

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
    void testCancelQueuedCampaign() throws Exception {
        var electionName = randomByteSequence();
        long leaderLease = leaseClient.grant(60).get(OPERATION_TIMEOUT, TimeUnit.SECONDS).getID();
        long contenderLease = leaseClient.grant(60).get(OPERATION_TIMEOUT, TimeUnit.SECONDS).getID();
        try {
            var leader = electionClient.campaign(electionName, leaderLease, randomByteSequence())
                .get(OPERATION_TIMEOUT, TimeUnit.SECONDS).getLeader();
            var contender = electionClient.campaign(electionName, contenderLease, randomByteSequence());
            var range = GetOption.builder().isPrefix(true).build();
            await().atMost(OPERATION_TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
                var queued = kvClient.get(electionName, range).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
                assertThat(queued.getKvs()).extracting(KeyValue::getLease).containsExactlyInAnyOrder(leaderLease,
                    contenderLease);
            });

            assertThat(contender.cancel(false)).isTrue();
            assertThat(contender).isCancelled();
            await().atMost(OPERATION_TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
                var queued = kvClient.get(electionName, range).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
                assertThat(queued.getKvs()).extracting(KeyValue::getKey).containsExactly(leader.key());
            });
        } finally {
            leaseClient.revoke(contenderLease).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            leaseClient.revoke(leaderLease).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        }
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
        assertThat(leaderResponse.getKv().getKey()).isEqualTo(campaignResponse1.getLeader().key());
        assertThat(campaignResponse1.getLeader().key()).isEqualTo(campaignResponse2.getLeader().key());
        assertThat(campaignResponse1.getLeader().revision()).isEqualTo(campaignResponse2.getLeader().revision());

        // latest proposal should be persisted
        GetOption getOption = GetOption.builder().isPrefix(true).build();
        List<KeyValue> keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).getValue()).isEqualTo(secondProposal);

        electionClient.resign(campaignResponse1.getLeader()).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        leaseClient.revoke(leaseId).get();
    }

    @Test
    public void testProposeValueNotBeingLeader() throws Exception {
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
        LeaderKey leaderKey = new LeaderKey(electionName, randomByteSequence(), 1, 1);
        ByteSequence proposal = ByteSequence.from("proposal", StandardCharsets.UTF_8);
        try {
            electionClient.proclaim(leaderKey, proposal).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            fail("Cannot proclaim proposal not being a leader");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(NotLeaderException.class);
        }
        GetOption getOption = GetOption.builder().isPrefix(true).build();
        List<KeyValue> keys = kvClient.get(electionName, getOption).get().getKvs();
        assertThat(keys).isEmpty();
    }

    @Test
    public void testObserveElections() throws Exception {
        int electionCount = 3;
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);

        var observer = new ElectionObserver();
        electionClient.observe(electionName, observer);
        long leaseId = leaseClient.grant(30).get(OPERATION_TIMEOUT, TimeUnit.SECONDS).getID();

        for (int i = 0; i < electionCount; ++i) {
            ByteSequence proposal = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
            CampaignResponse campaignResponse = electionClient.campaign(electionName, leaseId, proposal)
                .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            assertThat(observer.nextProposal()).isEqualTo(proposal);
            electionClient.resign(campaignResponse.getLeader()).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        }
        leaseClient.revoke(leaseId).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    public void testObserveExistingLeader() throws Exception {
        ByteSequence electionName = ByteSequence.from(randomString(), StandardCharsets.UTF_8);

        long leaseId = leaseClient.grant(30).get(OPERATION_TIMEOUT, TimeUnit.SECONDS).getID();

        ByteSequence proposal = ByteSequence.from(randomString(), StandardCharsets.UTF_8);
        electionClient.campaign(electionName, leaseId, proposal)
            .get(OPERATION_TIMEOUT, TimeUnit.SECONDS);

        var observer = new ElectionObserver();
        electionClient.observe(electionName, observer);
        assertThat(observer.nextProposal()).isEqualTo(proposal);
        leaseClient.revoke(leaseId).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
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
            Client client = TestUtil.client(cluster).build();
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
        GetOption getOption = GetOption.builder().isPrefix(true).build();
        assertThat(kvClient.get(electionName, getOption).get().getCount()).isEqualTo(0L);
        for (int i = 0; i < threadCount; ++i) {
            clients.get(i).getLeaseClient().revoke(leases.get(i)).get();
            clients.get(i).close();
        }
    }

    private static final class ElectionObserver implements Election.Listener {
        private final BlockingQueue<CompletableFuture<ByteSequence>> proposals = new LinkedBlockingQueue<>();

        @Override
        public void onNext(LeaderResponse response) {
            proposals.add(CompletableFuture.completedFuture(response.getKv().getValue()));
        }

        @Override
        public void onError(Throwable error) {
            proposals.add(CompletableFuture.failedFuture(error));
        }

        @Override
        public void onCompleted() {
            onError(new IllegalStateException("Election observation completed before the expected proclamation"));
        }

        ByteSequence nextProposal() throws InterruptedException, ExecutionException {
            var proposal = proposals.poll(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            assertThat(proposal).as("an observed proclamation").isNotNull();
            return proposal.get();
        }
    }
}
