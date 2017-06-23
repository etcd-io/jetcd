package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.api.Member;
import com.coreos.jetcd.api.MemberAddResponse;
import com.coreos.jetcd.api.MemberListResponse;
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
      throws ExecutionException, InterruptedException, AuthFailedException, ConnectException {
    Client client = ClientBuilder.newBuilder().endpoints(TestConstants.endpoints)
        .build();
    Cluster clusterClient = client.getClusterClient();
    MemberListResponse response = clusterClient.listMember().get();
    assertion.assertEquals(response.getMembersCount(), 3, "Members: " + response.getMembersCount());
  }

  /**
   * test add cluster function, added member will be removed by testDeleteMember
   */
  @Test(dependsOnMethods = "testListCluster")
  public void testAddMember()
      throws AuthFailedException, ConnectException, ExecutionException, InterruptedException, TimeoutException {
    Client client = ClientBuilder.newBuilder()
        .endpoints(Arrays.copyOfRange(TestConstants.endpoints, 0, 2)).build();
    Cluster clusterClient = client.getClusterClient();
    MemberListResponse response = clusterClient.listMember().get();
    assertion.assertEquals(response.getMembersCount(), 3);
    CompletableFuture<MemberAddResponse> responseListenableFuture = clusterClient
        .addMember(Arrays.asList(Arrays.copyOfRange(TestConstants.peerUrls, 2, 3)));
    MemberAddResponse addResponse = responseListenableFuture.get(5, TimeUnit.SECONDS);
    addedMember = addResponse.getMember();
    assertion.assertNotNull(addedMember, "added member: " + addedMember.getID());
  }

  /**
   * test update peer url for member
   */
  @Test(dependsOnMethods = "testAddMember")
  public void testUpdateMember() {

    Throwable throwable = null;
    try {
      Client client = ClientBuilder.newBuilder()
          .endpoints(Arrays.copyOfRange(TestConstants.endpoints, 1, 3)).build();
      Cluster clusterClient = client.getClusterClient();
      MemberListResponse response = clusterClient.listMember().get();
      String[] newPeerUrl = new String[]{"http://localhost:12380"};
      clusterClient.updateMember(response.getMembers(0).getID(), Arrays.asList(newPeerUrl)).get();
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
      throws ExecutionException, InterruptedException, AuthFailedException, ConnectException {
    Client client = ClientBuilder.newBuilder()
        .endpoints(Arrays.copyOfRange(TestConstants.endpoints, 0, 2)).build();
    Cluster clusterClient = client.getClusterClient();
    clusterClient.removeMember(addedMember.getID()).get();
    int newCount = clusterClient.listMember().get().getMembersCount();
    assertion.assertEquals(newCount, 3,
        "delete added member(" + addedMember.getID() + "), and left " + newCount + " members");
  }
}
