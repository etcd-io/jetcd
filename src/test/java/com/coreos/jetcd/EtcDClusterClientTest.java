package com.coreos.jetcd;

import com.coreos.jetcd.api.Member;
import com.coreos.jetcd.api.MemberAddResponse;
import com.coreos.jetcd.api.MemberListResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.google.common.util.concurrent.ListenableFuture;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * test etcd cluster client
 */
public class EtcDClusterClientTest extends EtcDClusterTest
{
    private Assertion assertion = new Assertion();
    private Member addedMember;


    /**
     * test list cluster function
     *
     * TODO: This assumes a fresh, predictable cluster (instance).  The JEtcD
     * TODO: test framework should probably be doing some environmental setup.
     * TODO: at88mph 2016.11.03
     */
    @Test
    public void testListCluster() throws ExecutionException,
                                         InterruptedException,
                                         AuthFailedException, ConnectException
    {
        final EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(getClusterEndpoints()).build();
        final EtcdCluster clusterClient = etcdClient.getClusterClient();
        final MemberListResponse response = clusterClient.listMember().get();

        assertion.assertEquals(response.getMembersCount(), 3,
                              "Members: " + response.getMembersCount());
    }

    /**
     * Test add cluster function, added member will be removed by
     * testDeleteMember.
     */
    @Test(dependsOnMethods = "testListCluster")
    public void testAddMember() throws AuthFailedException, ConnectException,
                                       ExecutionException, InterruptedException,
                                       TimeoutException
    {
        EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(Arrays.copyOfRange(getClusterEndpoints(), 0, 2))
                .build();
        EtcdCluster clusterClient = etcdClient.getClusterClient();
        MemberListResponse response = clusterClient.listMember().get();
        assertion.assertEquals(response.getMembersCount(), 3);
        ListenableFuture<MemberAddResponse> responseListenableFuture =
                clusterClient.addMember(Arrays.asList(Arrays.copyOfRange(
                        TestConstants.PEER_URLS, 2, 3)));
        MemberAddResponse addResponse = responseListenableFuture
                .get(5, TimeUnit.SECONDS);
        addedMember = addResponse.getMember();
        assertion.assertNotNull(addedMember, "added member: " + addedMember
                .getID());
    }

    /**
     * test update peer url for member
     */
    @Test(dependsOnMethods = "testAddMember")
    public void testUpdateMember() throws Exception
    {
        final EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(Arrays.copyOfRange(getClusterEndpoints(), 1, 3))
                .build();
        final EtcdCluster clusterClient = etcdClient.getClusterClient();
        final MemberListResponse response = clusterClient.listMember().get();
        final String[] newPeerUrl = new String[]{"http://localhost:12380"};

        clusterClient.updateMember(response.getMembers(0).getID(), Arrays
                .asList(newPeerUrl)).get();
    }

    /**
     * test remove member from cluster, the member is added by testAddMember
     */
    @Test(dependsOnMethods = "testUpdateMember")
    public void testDeleteMember() throws ExecutionException,
                                          InterruptedException,
                                          AuthFailedException, ConnectException
    {
        EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(Arrays.copyOfRange(getClusterEndpoints(), 0, 2))
                .build();
        EtcdCluster clusterClient = etcdClient.getClusterClient();
        clusterClient.removeMember(addedMember.getID()).get();
        int newCount = clusterClient.listMember().get().getMembersCount();
        assertion.assertEquals(newCount, 3, "delete added member(" + addedMember
                .getID() + "), and left " + newCount + " members");
    }
}
