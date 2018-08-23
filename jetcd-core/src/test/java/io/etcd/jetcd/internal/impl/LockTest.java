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
package io.etcd.jetcd.internal.impl;

import com.google.common.base.Charsets;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.internal.infrastructure.EtcdCluster;
import io.etcd.jetcd.internal.infrastructure.EtcdClusterFactory;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lock.LockResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * Lock service test cases.
 */
public class LockTest {
  private static final EtcdCluster CLUSTER = EtcdClusterFactory.buildCluster("etcd-lock", 3 ,false);

  private Lock lockClient;
  private Lease leaseClient;
  private Assertion test;
  private Set<ByteSequence> locksToRelease;

  private static final ByteSequence SAMPLE_NAME = ByteSequence.from("sample_name", Charsets.UTF_8);

  @BeforeTest
  public void setUp() throws Exception {
    CLUSTER.start();

    Client client = Client.builder().endpoints(CLUSTER.getClientEndpoints()).build();

    test = new Assertion();
    lockClient = client.getLockClient();
    leaseClient = client.getLeaseClient();
  }

  @BeforeMethod
  public void setUpEach() throws Exception {
    locksToRelease = new HashSet<>();
  }

  @AfterMethod
  public void tearDownEach() throws Exception {
    for (ByteSequence lockKey : locksToRelease) {
      lockClient.unlock(lockKey).get();
    }
  }

  @Test
  public void testLockWithoutLease() throws Exception {
    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, 0);
    LockResponse response = feature.get();
    locksToRelease.add(response.getKey());

    test.assertTrue(response.getHeader() != null);
    test.assertTrue(response.getKey() != null);
  }

  @Test(expectedExceptions = ExecutionException.class,
      expectedExceptionsMessageRegExp = ".*etcdserver: requested lease not found")
  public void testLockWithNotExistingLease() throws Exception {
    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, 123456);
    LockResponse response = feature.get();
    locksToRelease.add(response.getKey());
  }

  @Test
  public void testLockWithLease() throws Exception {
    long lease = grantLease(5);
    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, lease);
    LockResponse response = feature.get();

    long startMillis = System.currentTimeMillis();

    CompletableFuture<LockResponse> feature2 = lockClient.lock(SAMPLE_NAME, 0);
    LockResponse response2 = feature2.get();

    long time = System.currentTimeMillis() - startMillis;

    test.assertNotEquals(response.getKey(), response2.getKey());
    test.assertTrue(time >= 4500 && time <= 6000,
        String.format("Lease not runned out after 5000ms, was %dms", time));

    locksToRelease.add(response.getKey());
    locksToRelease.add(response2.getKey());
  }

  @Test
  public void testLockAndUnlock() throws Exception {
    long lease = grantLease(20);
    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, lease);
    LockResponse response = feature.get();

    lockClient.unlock(response.getKey()).get();

    long startTime = System.currentTimeMillis();

    CompletableFuture<LockResponse> feature2 = lockClient.lock(SAMPLE_NAME, 0);
    LockResponse response2 = feature2.get();

    long time = System.currentTimeMillis() - startTime;

    locksToRelease.add(response2.getKey());

    test.assertNotEquals(response.getKey(), response2.getKey());
    test.assertTrue(time <= 500,
        String.format("Lease not unlocked, wait time was too long (%dms)", time));
  }

  private long grantLease(long ttl) throws Exception {
    CompletableFuture<LeaseGrantResponse> feature = leaseClient.grant(ttl);
    LeaseGrantResponse response = feature.get();
    return response.getID();
  }

  @AfterTest
  public void tearDown() throws IOException {
    CLUSTER.close();
  }
}
