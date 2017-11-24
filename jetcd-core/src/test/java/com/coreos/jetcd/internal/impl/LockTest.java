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

import com.coreos.jetcd.*;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lock.LockResponse;
import org.testng.annotations.*;
import org.testng.asserts.Assertion;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Lock service test cases.
 */
public class LockTest {

  private Lock lockClient;
  private Lease leaseClient;
  private Assertion test;
  private Set<ByteSequence> locksToRelease;

  private static final ByteSequence SAMPLE_NAME = ByteSequence.fromString("sample_name");

  @BeforeTest
  public void setUp() throws Exception {
    test = new Assertion();
    Client client = Client.builder().endpoints(TestConstants.endpoints).build();
    lockClient = client.getLockClient();
    leaseClient = client.getLeaseClient();
    locksToRelease = new HashSet<>();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    for (ByteSequence lockKey : locksToRelease) {
      lockClient.unlock(lockKey).get();
    }
    locksToRelease.clear();
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
    long lease = grantLease(20);

    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, lease);
    LockResponse response = feature.get();

    long currentMillis = System.currentTimeMillis();

    CompletableFuture<LockResponse> feature2 = lockClient.lock(SAMPLE_NAME, 0);
    LockResponse response2 = feature2.get();

    long time = System.currentTimeMillis() - currentMillis;

    test.assertNotEquals(response.getKey(), response2.getKey());
    test.assertTrue(time >= 19500 && time <= 21000,
            String.format("Lease not runned out after 20000ms, was %dms", time));

    locksToRelease.add(response.getKey());
    locksToRelease.add(response2.getKey());
  }

  @Test
  public void testLockWithUnlock() throws Exception {
    long lease = grantLease(20);
    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, lease);
    LockResponse response = feature.get();

    Thread.sleep(5000);

    lockClient.unlock(response.getKey()).get();

    long currentMillis = System.currentTimeMillis();

    CompletableFuture<LockResponse> feature2 = lockClient.lock(SAMPLE_NAME, 0);
    LockResponse response2 = feature2.get();

    long time = System.currentTimeMillis() - currentMillis;

    test.assertNotEquals(response.getKey(), response2.getKey());
    test.assertTrue(time <= 500,
            String.format("Lease not unlocked, wait time was too long (%dms)", time));

    locksToRelease.add(response2.getKey());
  }

  private long grantLease(long ttl) throws Exception {
    CompletableFuture<LeaseGrantResponse> feature = leaseClient.grant(ttl);
    LeaseGrantResponse response = feature.get();
    return response.getID();
  }

}
