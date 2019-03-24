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
package io.etcd.jetcd;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import io.etcd.jetcd.launcher.junit5.EtcdClusterExtension;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.options.PutOption;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Lease service test cases.
 */
public class LeaseTest {

  @RegisterExtension
  public static final EtcdClusterExtension cluster = new EtcdClusterExtension("etcd-lease", 3 ,false);

  private KV kvClient;
  private Client client;
  private Lease leaseClient;

  private static final ByteSequence KEY = ByteSequence.from("foo", Charsets.UTF_8);
  private static final ByteSequence KEY_2 = ByteSequence.from("foo2", Charsets.UTF_8);
  private static final ByteSequence VALUE = ByteSequence.from("bar", Charsets.UTF_8);


  @BeforeEach
  public void setUp() {
    client = Client.builder().endpoints(cluster.getClientEndpoints()).build();
    kvClient = client.getKVClient();
    leaseClient = client.getLeaseClient();
  }

  @AfterEach
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

  @Test
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
