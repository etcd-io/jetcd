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

import com.google.common.base.Charsets;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
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
import org.testng.annotations.DataProvider;
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
  private static final ByteSequence namespace = ByteSequence.from("test-ns/", Charsets.UTF_8);
  private static final ByteSequence namespace2 = ByteSequence.from("test-ns2/", Charsets.UTF_8);

  @DataProvider(name = "NamespaceTest")
  public static Object[][] parameters() {
    return new Boolean[][] {{true}, {false}};
  }

  @BeforeTest
  public void setUp() throws Exception {
    CLUSTER.start();

    Client client = Client.builder().endpoints(CLUSTER.getClientEndpoints()).build();

    test = new Assertion();
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

  private void initializeLockCLient(boolean useNamespace) {
    Client client = useNamespace
        ? Client.builder().endpoints(CLUSTER.getClientEndpoints()).namespace(namespace).build()
        : Client.builder().endpoints(CLUSTER.getClientEndpoints()).build();
    this.lockClient = client.getLockClient();
  }

  @Test(dataProvider = "NamespaceTest")
  public void testLockWithoutLease(boolean useNamespace) throws Exception {
    initializeLockCLient(useNamespace);
    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, 0);
    LockResponse response = feature.get();
    locksToRelease.add(response.getKey());

    test.assertTrue(response.getHeader() != null);
    test.assertTrue(response.getKey() != null);
  }

  @Test(expectedExceptions = ExecutionException.class,
      expectedExceptionsMessageRegExp = ".*etcdserver: requested lease not found",
      dataProvider = "NamespaceTest")
  public void testLockWithNotExistingLease(boolean useNamespace) throws Exception {
    initializeLockCLient(useNamespace);
    CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, 123456);
    LockResponse response = feature.get();
    locksToRelease.add(response.getKey());
  }

  @Test(dataProvider = "NamespaceTest")
  public void testLockWithLease(boolean useNamespace) throws Exception {
    initializeLockCLient(useNamespace);
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

  @Test(dataProvider = "NamespaceTest")
  public void testLockAndUnlock(boolean useNamespace) throws Exception {
    initializeLockCLient(useNamespace);
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

  @Test
  public void testLockSegregationByNamespaces() throws Exception {
    initializeLockCLient(false);

    // prepare two LockClients with different namespaces, lock operations on one LockClient
    // should have no effect on the other client.
    Client clientWithNamespace = Client.builder().endpoints(CLUSTER.getClientEndpoints())
        .namespace(namespace).build();
    Lock lockClientWithNamespace = clientWithNamespace.getLockClient();

    Client clientWithNamespace2 = Client.builder().endpoints(CLUSTER.getClientEndpoints())
        .namespace(namespace2).build();
    Lock lockClientWithNamespace2 = clientWithNamespace2.getLockClient();

    long lease = grantLease(5);
    CompletableFuture<LockResponse> feature = lockClientWithNamespace.lock(SAMPLE_NAME, lease);
    LockResponse response = feature.get();

    long startTime = System.currentTimeMillis();

    test.assertTrue(response.getKey().startsWith(SAMPLE_NAME));

    // Unlock by full key name using LockClient without namespace, thus it should not take
    // much time to lock the same key again.
    ByteSequence wKey = ByteSequence.from(namespace.getByteString().concat(
        response.getKey().getByteString()));
    lockClient.unlock(wKey).get();
    lease = grantLease(30);
    CompletableFuture<LockResponse> feature2 = lockClientWithNamespace.lock(SAMPLE_NAME, lease);
    LockResponse response2 = feature2.get();

    long timestamp2 = System.currentTimeMillis();

    test.assertTrue(response2.getKey().startsWith(SAMPLE_NAME));
    test.assertNotEquals(response.getKey(), response2.getKey());
    test.assertTrue((timestamp2 - startTime) <= 1000, String.format(
        "Lease not unlocked, wait time was too long (%dms)", (timestamp2 - startTime)));

    locksToRelease.add(ByteSequence.from(namespace.getByteString().concat(
        response2.getKey().getByteString())));

    // Lock the same key using LockClient with another namespace, it also should not take much time.
    lease = grantLease(5);
    CompletableFuture<LockResponse> feature3 = lockClientWithNamespace2.lock(SAMPLE_NAME, lease);
    LockResponse response3 = feature3.get();

    long timestamp3 = System.currentTimeMillis();

    test.assertTrue(response3.getKey().startsWith(SAMPLE_NAME));
    test.assertNotEquals(response3.getKey(), response2.getKey());
    test.assertTrue((timestamp3 - timestamp2) <= 1000, String.format(
        "wait time for requiring the lock was too long (%dms)", (timestamp3 - timestamp2)));

    locksToRelease.add(ByteSequence.from(namespace2.getByteString().concat(
        response3.getKey().getByteString())));
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
