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
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * test etcd cluster client
 */
public class EtcdClusterClientTest extends AbstractTest
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
        final String[] endpoints = getEndpoints();
        EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(endpoints).build();
        EtcdCluster clusterClient = etcdClient.getClusterClient();

        MemberListResponse response = clusterClient.listMember().get();
        assertion.assertEquals(response.getMembersCount(),
                               endpoints.length, "Members: "
                                                 + response.getMembersCount());
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
        final String[] endpoints = getEndpoints();
        EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(endpoints).build();
        EtcdCluster clusterClient = etcdClient.getClusterClient();
        MemberListResponse response = clusterClient.listMember().get();
        assertion.assertEquals(response.getMembersCount(),
                               endpoints.length);
        ListenableFuture<MemberAddResponse> responseListenableFuture =
                clusterClient.addMember(
                        Collections.singletonList("http://localhost:4001"));
//                clusterClient.addMember(Arrays.asList(Arrays.copyOfRange(
//                        TestConstants.peerUrls, 2, 3)));
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
    public void testUpdateMember()
    {
        final String[] endpoints = getEndpoints();
        if (endpoints.length > 1)
        {
            Throwable throwable = null;
            try
            {
                EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                        .endpoints(endpoints).build();
                EtcdCluster clusterClient = etcdClient.getClusterClient();
                MemberListResponse response = clusterClient.listMember().get();
                String[] newPeerUrl = new String[]{"http://localhost:12380"};
                clusterClient
                        .updateMember(response.getMembers(0).getID(), Arrays
                                .asList(newPeerUrl)).get();
            }
            catch (Exception e)
            {
                System.out.println(e);
                throwable = e;
            }
            assertion.assertNull(throwable, "update for member");
        }
    }

    /**
     * test remove member from cluster, the member is added by testAddMember
     */
    @Test(dependsOnMethods = "testUpdateMember")
    public void testDeleteMember() throws ExecutionException,
                                          InterruptedException,
                                          AuthFailedException, ConnectException
    {
        final String[] endpoints = getEndpoints();
        EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(endpoints).build();
        EtcdCluster clusterClient = etcdClient.getClusterClient();
        clusterClient.removeMember(addedMember.getID()).get();
        int newCount = clusterClient.listMember().get().getMembersCount();
        assertion.assertEquals(newCount, endpoints.length,
                               "delete added member("
                               + addedMember.getID() + "), and left "
                               + newCount + " members");
    }
}
