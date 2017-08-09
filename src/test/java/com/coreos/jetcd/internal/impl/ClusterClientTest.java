package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.cluster.Member;
import com.coreos.jetcd.cluster.MemberAddResponse;
import com.coreos.jetcd.cluster.MemberListResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * test etcd cluster client
 */
public class ClusterClientTest {

  private Assertion assertion = new Assertion();
  private Member addedMember;

  /**
   * test list cluster function
   */
  @Test
  public void testListCluster()
      throws ExecutionException, InterruptedException {
    Client client = Client.builder().endpoints(TestConstants.endpoints).build();
    Cluster clusterClient = client.getClusterClient();
    MemberListResponse response = clusterClient.listMember().get();
    assertion
        .assertEquals(response.getMembers().size(), 3, "Members: " + response.getMembers().size());
  }

  /**
   * test add cluster function, added member will be removed by testDeleteMember
   */
  @Test(dependsOnMethods = "testListCluster")
  public void testAddMember()
      throws AuthFailedException, ConnectException, ExecutionException, InterruptedException, TimeoutException {
    Client client = Client.builder()
        .endpoints(Arrays.copyOfRange(TestConstants.endpoints, 0, 2))
        .build();

    Cluster clusterClient = client.getClusterClient();
    MemberListResponse response = clusterClient.listMember().get();
    assertion.assertEquals(response.getMembers().size(), 3);
    CompletableFuture<MemberAddResponse> responseListenableFuture = clusterClient
        .addMember(Arrays.asList(Arrays.copyOfRange(TestConstants.peerUrls, 2, 3)));
    MemberAddResponse addResponse = responseListenableFuture.get(5, TimeUnit.SECONDS);
    addedMember = addResponse.getMember();
    assertion.assertNotNull(addedMember, "added member: " + addedMember.getId());
  }

  /**
   * test update peer url for member
   */
  @Test(dependsOnMethods = "testAddMember")
  public void testUpdateMember() {

    Throwable throwable = null;
    try {
      Client client = Client.builder()
          .endpoints(Arrays.copyOfRange(TestConstants.endpoints, 1, 3))
          .build();

      Cluster clusterClient = client.getClusterClient();
      MemberListResponse response = clusterClient.listMember().get();
      String[] newPeerUrl = new String[]{"http://localhost:12380"};
      clusterClient.updateMember(response.getMembers().get(0).getId(), Arrays.asList(newPeerUrl))
          .get();
    } catch (Exception e) {
      System.out.println(e);
      throwable = e;
    }
    assertion.assertNull(throwable, "update for member");
  }

  /**
   * test remove member from cluster, the member is added by testAddMember
   */
  @Test(dependsOnMethods = "testUpdateMember")
  public void testDeleteMember()
      throws ExecutionException, InterruptedException {
    Client client = Client.builder()
        .endpoints(Arrays.copyOfRange(TestConstants.endpoints, 0, 2))
        .build();

    Cluster clusterClient = client.getClusterClient();
    clusterClient.removeMember(addedMember.getId()).get();
    int newCount = clusterClient.listMember().get().getMembers().size();
    assertion.assertEquals(newCount, 3,
        "delete added member(" + addedMember.getId() + "), and left " + newCount + " members");
  }
}
