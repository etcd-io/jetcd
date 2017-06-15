package com.coreos.jetcd;

import static org.assertj.core.api.Assertions.assertThat;

import com.coreos.jetcd.Lease.LeaseHandler;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.api.PutResponse;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.options.LeaseOption;
import com.coreos.jetcd.options.PutOption;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * KV service test cases.
 */
public class LeaseTest {

  private KV kvClient;
  private Client client;
  private Lease leaseClient;
  private Assertion test;


  private static final ByteSequence KEY = ByteSequence.fromString("foo");
  private static final ByteSequence KEY_2 = ByteSequence.fromString("foo2");
  private static final ByteSequence VALUE = ByteSequence.fromString("bar");

  @BeforeTest
  public void setUp() throws Exception {
    test = new Assertion();
    client = ClientBuilder.newBuilder().endpoints(TestConstants.endpoints).build();
    kvClient = client.getKVClient();
    leaseClient = client.getLeaseClient();
  }

  @Test
  public void testGrant() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();
    PutResponse putRep = kvClient
        .put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    test.assertEquals(kvClient.get(KEY).get().getCount(), 1);

    Thread.sleep(6000);
    test.assertEquals(kvClient.get(KEY).get().getCount(), 0);
  }

  @Test(dependsOnMethods = "testGrant")
  public void testRevoke() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();
    PutResponse putRep = kvClient
        .put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    test.assertEquals(kvClient.get(KEY).get().getCount(), 1);
    leaseClient.revoke(leaseID).get();
    test.assertEquals(kvClient.get(KEY).get().getCount(), 0);
  }

  @Test(dependsOnMethods = "testRevoke")
  public void testKeepAliveOnce() throws ExecutionException, InterruptedException {
    long leaseID = leaseClient.grant(2).get().getID();
    kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    test.assertEquals(kvClient.get(KEY).get().getCount(), 1);
    LeaseKeepAliveResponse rp = leaseClient.keepAliveOnce(leaseID).get();
    assertThat(rp.getTTL()).isGreaterThan(0);
  }

  @Test(dependsOnMethods = "testKeepAliveOnce")
  public void testkeepAlive() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();
    PutResponse putRep = kvClient
        .put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    test.assertEquals(kvClient.get(KEY).get().getCount(), 1);
    leaseClient.startKeepAliveService();
    leaseClient.keepAlive(leaseID, new LeaseHandler() {
      @Override
      public void onKeepAliveRespond(LeaseKeepAliveResponse keepAliveResponse) {

      }

      @Override
      public void onLeaseExpired(long leaseId) {

      }

      @Override
      public void onError(Throwable throwable) {

      }
    });
    Thread.sleep(6000);
    test.assertEquals(kvClient.get(KEY).get().getCount(), 1);
    leaseClient.cancelKeepAlive(leaseID);
    test.assertEquals(kvClient.get(KEY).get().getCount(), 0);
  }

  @Test
  public void testTimeToLive() throws ExecutionException, InterruptedException {
    long ttl = 5;
    long leaseID = leaseClient.grant(ttl).get().getID();
    LeaseTimeToLiveResponse resp = leaseClient.timeToLive(leaseID, LeaseOption.DEFAULT).get();
    assertThat(resp.getTTl()).isGreaterThan(0);
    assertThat(resp.getGrantedTTL()).isEqualTo(ttl);
  }

  @Test
  public void testTimeToLiveWithKeys() throws ExecutionException, InterruptedException {
    long ttl = 5;
    long leaseID = leaseClient.grant(ttl).get().getID();
    PutOption putOption = PutOption.newBuilder().withLeaseId(leaseID).build();
    kvClient.put(KEY_2, VALUE, putOption).get();

    LeaseOption leaseOption = LeaseOption.newBuilder().withAttachedKeys().build();
    LeaseTimeToLiveResponse resp = leaseClient.timeToLive(leaseID, leaseOption).get();
    assertThat(resp.getTTl()).isGreaterThan(0);
    assertThat(resp.getGrantedTTL()).isEqualTo(ttl);
    assertThat(resp.getKeys().size()).isEqualTo(1);
    assertThat(resp.getKeys().get(0)).isEqualTo(KEY_2);
  }
}
