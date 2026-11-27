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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.cluster.Member;
import io.etcd.jetcd.cluster.MemberPromoteResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.etcd.jetcd.test.EtcdConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class ClusterMembersTest {
    // These tests register a member without starting an etcd node that joins the cluster.
    private static final List<URI> UNJOINED_PEER = List.of(URI.create("http://" + EtcdConstants.LOCALHOST + ":1"));

    @RegisterExtension
    public final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    @Test
    public void testMemberManagement() throws ExecutionException, InterruptedException, TimeoutException {
        try (Client client = TestUtil.client(cluster).build()) {
            final Cluster clusterClient = client.getClusterClient();

            Member m2 = clusterClient.addMember(UNJOINED_PEER)
                .get(5, TimeUnit.SECONDS)
                .getMember();

            assertThat(m2).isNotNull();
            assertThat(m2.getPeerURIs()).containsExactlyElementsOf(UNJOINED_PEER);
            assertThat(clusterClient.listMember().get(5, TimeUnit.SECONDS).getMembers()).hasSize(2);
        }
    }

    @Test
    public void testMemberManagementAddNonLearner() throws ExecutionException, InterruptedException, TimeoutException {
        try (Client client = TestUtil.client(cluster).build()) {
            final Cluster clusterClient = client.getClusterClient();

            Member m2 = clusterClient.addMember(UNJOINED_PEER, false)
                .get(5, TimeUnit.SECONDS)
                .getMember();

            assertThat(m2).isNotNull();
            assertThat(m2.isLearner()).isFalse();

            List<Member> members = clusterClient.listMember().get(5, TimeUnit.SECONDS).getMembers();
            assertThat(members).hasSize(2);
            assertThat(members.stream().filter(Member::isLearner).findAny()).isEmpty();
        }
    }

    @Test
    public void testMemberManagementAddLearner() throws ExecutionException, InterruptedException, TimeoutException {
        try (Client client = TestUtil.client(cluster).build()) {
            final Cluster clusterClient = client.getClusterClient();

            Member m2 = clusterClient.addMember(UNJOINED_PEER, true)
                .get(5, TimeUnit.SECONDS)
                .getMember();

            assertThat(m2).isNotNull();
            assertThat(m2.isLearner()).isTrue();

            List<Member> members = clusterClient.listMember().get(5, TimeUnit.SECONDS).getMembers();
            assertThat(members).hasSize(2);
            assertThat(members.stream().filter(Member::isLearner).findAny()).isPresent();
        }
    }

    @Test
    public void testMemberManagementAddLearnerAndPromote() throws ExecutionException, InterruptedException, TimeoutException {
        try (Client client = TestUtil.client(cluster).build()) {
            final Cluster clusterClient = client.getClusterClient();

            Member m2 = clusterClient.addMember(UNJOINED_PEER, true)
                .get(5, TimeUnit.SECONDS)
                .getMember();

            assertThat(m2).isNotNull();
            assertThat(m2.isLearner()).isTrue();

            // Now attempt to promote a member; although it fails, it confirms that the API was executed.
            Future<MemberPromoteResponse> promoteResponseFuture = clusterClient.promoteMember(m2.getId());
            assertThatExceptionOfType(ExecutionException.class)
                .isThrownBy(() -> promoteResponseFuture.get(5, TimeUnit.SECONDS)).withMessageContaining("EtcdException");
        }
    }
}
