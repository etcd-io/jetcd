/*
 * Copyright 2016-2019 The jetcd authors
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

import static org.assertj.core.api.Assertions.assertThat;

import io.etcd.jetcd.cluster.Member;
import io.etcd.jetcd.cluster.MemberAddResponse;
import io.etcd.jetcd.cluster.MemberListResponse;
import io.etcd.jetcd.launcher.junit5.EtcdClusterExtension;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ClusterClientTest {

  @RegisterExtension
  public static EtcdClusterExtension cluster = new EtcdClusterExtension("cluster-client", 3 ,false);

  private static List<URI> endpoints;
  private static List<URI> peerUrls;

  @BeforeAll
  public static void setUp() throws InterruptedException {
    endpoints = cluster.getClientEndpoints();
    peerUrls = cluster.getPeerEndpoints();
    TimeUnit.SECONDS.sleep(5);
  }

  @Test
  public void testListCluster() throws ExecutionException, InterruptedException {
    Client client = Client.builder().endpoints(endpoints).build();
    Cluster clusterClient = client.getClusterClient();
    MemberListResponse response = clusterClient.listMember().get();
    assertThat(response.getMembers()).hasSize(3);
  }

  @Test
  public void testMemberManagement() throws ExecutionException, InterruptedException, TimeoutException {
    final Client client = Client.builder()
        .endpoints(endpoints.subList(0, 2))
        .build();
    final Cluster clusterClient = client.getClusterClient();

    MemberListResponse response = clusterClient.listMember().get();
    assertThat(response.getMembers()).hasSize(3);

    CompletableFuture<MemberAddResponse> responseListenableFuture = clusterClient.addMember(peerUrls.subList(2, 3));
    MemberAddResponse addResponse = responseListenableFuture.get(5, TimeUnit.SECONDS);
    final Member addedMember = addResponse.getMember();
    assertThat(addedMember).isNotNull();
    assertThat(clusterClient.listMember().get().getMembers()).hasSize(4);

    // Test update peer url for member
    response = clusterClient.listMember().get();
    List<URI> newPeerUrls = peerUrls.subList(0, 1);
    clusterClient.updateMember(response.getMembers().get(0).getId(), newPeerUrls).get();
    response = clusterClient.listMember().get();
    assertThat(response.getMembers().get(0).getPeerURIs()).containsOnly(newPeerUrls.toArray(new URI[0]));

    // Test remove member from cluster, the member is added by testAddMember
    clusterClient.removeMember(addedMember.getId()).get();
    assertThat(clusterClient.listMember().get().getMembers()).hasSize(3);
  }

}
