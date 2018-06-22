/**
 * Copyright 2017 The jetcd authors
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
package com.coreos.jetcd.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.internal.infrastructure.EtcdCluster;
import com.coreos.jetcd.internal.infrastructure.EtcdClusterFactory;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.lease.LeaseTimeToLiveResponse;
import com.coreos.jetcd.options.LeaseOption;
import com.coreos.jetcd.options.PutOption;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Lease service test cases.
 */
public class LeaseTest {
  private static EtcdCluster CLUSTER;

  private KV kvClient;
  private Client client;
  private Lease leaseClient;

  private static final ByteSequence KEY = ByteSequence.fromString("foo");
  private static final ByteSequence KEY_2 = ByteSequence.fromString("foo2");
  private static final ByteSequence VALUE = ByteSequence.fromString("bar");

  @BeforeClass
  public static void beforeClass() {
    CLUSTER = EtcdClusterFactory.buildCluster("etcd-lease", 3 ,false);
    CLUSTER.start();
  }

  @AfterClass
  public static void afterClass() {
    CLUSTER.close();
  }

  @Before
  public void setUp() {
    CLUSTER.start();

    client = Client.builder().endpoints(CLUSTER.getClientEndpoints()).build();
    kvClient = client.getKVClient();
    leaseClient = client.getLeaseClient();
  }

  @After
  public void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void testGrant() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();

    kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);

    Thread.sleep(6000);
    assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(0);
  }

  @Test//(dependsOnMethods = "testGrant")
  public void testRevoke() throws Exception {
    long leaseID = leaseClient.grant(5).get().getID();
    kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);
    leaseClient.revoke(leaseID).get();
    assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(0);
  }

  @Test
  public void testKeepAliveOnce() throws ExecutionException, InterruptedException {
    long leaseID = leaseClient.grant(2).get().getID();
    kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);
    LeaseKeepAliveResponse rp = leaseClient.keepAliveOnce(leaseID).get();
    assertThat(rp.getTTL()).isGreaterThan(0);
  }

  @Test
  public void testKeepAlive() throws ExecutionException, InterruptedException {
    long leaseID = leaseClient.grant(2).get().getID();
    kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<LeaseKeepAliveResponse> responseRef = new AtomicReference<>();
    StreamObserver<LeaseKeepAliveResponse> observer = Observers.observer(
      response -> {
        responseRef.set(response);
        latch.countDown();
      }
    );

    try(CloseableClient c = leaseClient.keepAlive(leaseID, observer)) {
      latch.await(5, TimeUnit.SECONDS);
      LeaseKeepAliveResponse response = responseRef.get();
      assertThat(response.getTTL()).isGreaterThan(0);
    }

    Thread.sleep(3000);
    assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(0);
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
