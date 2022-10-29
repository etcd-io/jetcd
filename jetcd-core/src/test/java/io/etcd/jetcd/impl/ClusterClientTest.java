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
import org.testcontainers.containers.Network;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.cluster.Member;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class ClusterClientTest {
    private static final Network NETWORK = Network.newNetwork();

    @RegisterExtension
    public static final EtcdClusterExtension n1 = EtcdClusterExtension.builder()
        .withNodes(1)
        .withPrefix("n1")
        .withNetwork(NETWORK)
        .build();
    @RegisterExtension
    public static final EtcdClusterExtension n2 = EtcdClusterExtension.builder()
        .withNodes(1)
        .withPrefix("n2")
        .withNetwork(NETWORK)
        .build();
    @RegisterExtension
    public static final EtcdClusterExtension n3 = EtcdClusterExtension.builder()
        .withNodes(1)
        .withPrefix("n3")
        .withNetwork(NETWORK)
        .build();

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(3)
        .withPrefix("cluster")
        .build();

    @Test
    public void testMemberList() throws ExecutionException, InterruptedException {
        try (Client client = TestUtil.client(cluster).build()) {
            assertThat(client.getClusterClient().listMember().get().getMembers()).hasSize(3);
        }
    }

    @Test
    public void testMemberManagement() throws ExecutionException, InterruptedException, TimeoutException {
        final Client client = Client.builder().endpoints(n1.clientEndpoints()).build();
        final Cluster clusterClient = client.getClusterClient();

        Member m2 = clusterClient.addMember(n2.peerEndpoints(), false)
            .get(5, TimeUnit.SECONDS)
            .getMember();

        assertThat(m2).isNotNull();
        assertThat(clusterClient.listMember().get().getMembers()).hasSize(2);

        /*
        TODO: check
        Member m3 = clusterClient.addMember(n3.peerEndpoints())
            .get(5, TimeUnit.SECONDS)
            .getMember();
        
        assertThat(m3).isNotNull();
        assertThat(clusterClient.listMember().get().getMembers()).hasSize(3);
        */
    }
}
