/*
 * Copyright 2016-2021 The jetcd authors
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.google.common.base.Charsets;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LockTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = new EtcdClusterExtension("etcd-lock", 3, false);

    private Lock lockClient;
    private static Lease leaseClient;
    private Set<ByteSequence> locksToRelease;

    private static final ByteSequence SAMPLE_NAME = ByteSequence.from("sample_name", Charsets.UTF_8);
    private static final ByteSequence namespace = ByteSequence.from("test-ns/", Charsets.UTF_8);
    private static final ByteSequence namespace2 = ByteSequence.from("test-ns2/", Charsets.UTF_8);

    @BeforeAll
    public static void setUp() {
        Client client = Client.builder().endpoints(cluster.getClientEndpoints()).build();

        leaseClient = client.getLeaseClient();
    }

    @BeforeEach
    public void setUpEach() {
        locksToRelease = new HashSet<>();
    }

    @AfterEach
    public void tearDownEach() throws Exception {
        for (ByteSequence lockKey : locksToRelease) {
            lockClient.unlock(lockKey).get();
        }
    }

    private void initializeLockCLient(boolean useNamespace) {
        Client client = useNamespace ? Client.builder().endpoints(cluster.getClientEndpoints()).namespace(namespace).build()
            : Client.builder().endpoints(cluster.getClientEndpoints()).build();
        this.lockClient = client.getLockClient();
    }

    static Stream<Arguments> parameters() {
        return Stream.of(Arguments.of(true), Arguments.of(false));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLockWithoutLease(boolean useNamespace) throws Exception {
        initializeLockCLient(useNamespace);
        CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, 0);
        LockResponse response = feature.get();
        locksToRelease.add(response.getKey());

        assertThat(response.getHeader()).isNotNull();
        assertThat(response.getKey()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLockWithNotExistingLease(boolean useNamespace) {
        Throwable exception = assertThrows(ExecutionException.class, () -> {
            initializeLockCLient(useNamespace);
            CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, 123456);
            LockResponse response = feature.get();
            locksToRelease.add(response.getKey());
        });
        assertThat(exception.getMessage().contains("etcdserver: requested lease not found")).isTrue();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLockWithLease(boolean useNamespace) throws Exception {
        initializeLockCLient(useNamespace);
        long lease = grantLease(5);
        CompletableFuture<LockResponse> feature = lockClient.lock(SAMPLE_NAME, lease);
        LockResponse response = feature.get();

        long startMillis = System.currentTimeMillis();

        CompletableFuture<LockResponse> feature2 = lockClient.lock(SAMPLE_NAME, 0);
        LockResponse response2 = feature2.get();

        long time = System.currentTimeMillis() - startMillis;

        assertThat(response2.getKey()).isNotEqualTo(response.getKey());
        assertThat(time >= 4500 && time <= 6000)
            .withFailMessage(String.format("Lease not runned out after 5000ms, was %dms", time)).isTrue();

        locksToRelease.add(response.getKey());
        locksToRelease.add(response2.getKey());
    }

    @ParameterizedTest
    @MethodSource("parameters")
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

        assertThat(response2.getKey()).isNotEqualTo(response.getKey());
        assertThat(time <= 500).withFailMessage(String.format("Lease not unlocked, wait time was too long (%dms)", time))
            .isTrue();
    }

    @Test
    public void testLockSegregationByNamespaces() throws Exception {
        initializeLockCLient(false);

        // prepare two LockClients with different namespaces, lock operations on one LockClient
        // should have no effect on the other client.
        Client clientWithNamespace = Client.builder().endpoints(cluster.getClientEndpoints()).namespace(namespace).build();
        Lock lockClientWithNamespace = clientWithNamespace.getLockClient();

        long lease = grantLease(5);
        CompletableFuture<LockResponse> feature = lockClientWithNamespace.lock(SAMPLE_NAME, lease);
        LockResponse response = feature.get();

        assertThat(response.getKey().startsWith(SAMPLE_NAME)).isTrue();

        // Unlock by full key name using LockClient without namespace, thus it should not take
        // much time to lock the same key again.
        ByteSequence nsKey = ByteSequence.from(namespace.getByteString().concat(response.getKey().getByteString()));
        lockClient.unlock(nsKey).get();
        lease = grantLease(30);
        CompletableFuture<LockResponse> feature2 = lockClientWithNamespace.lock(SAMPLE_NAME, lease);
        LockResponse response2 = feature2.get();

        long timestamp2 = System.currentTimeMillis();

        long startTime = System.currentTimeMillis();
        assertThat(response2.getKey().startsWith(SAMPLE_NAME)).isTrue();
        assertThat(response2.getKey()).isNotEqualTo(response.getKey());
        assertThat((timestamp2 - startTime) <= 1000)
            .withFailMessage(String.format("Lease not unlocked, wait time was too long (%dms)", (timestamp2 - startTime)))
            .isTrue();

        locksToRelease.add(ByteSequence.from(namespace.getByteString().concat(response2.getKey().getByteString())));

        // Lock the same key using LockClient with another namespace, it also should not take much time.
        lease = grantLease(5);
        Client clientWithNamespace2 = Client.builder().endpoints(cluster.getClientEndpoints()).namespace(namespace2).build();
        Lock lockClientWithNamespace2 = clientWithNamespace2.getLockClient();
        CompletableFuture<LockResponse> feature3 = lockClientWithNamespace2.lock(SAMPLE_NAME, lease);
        LockResponse response3 = feature3.get();

        long timestamp3 = System.currentTimeMillis();

        assertThat(response3.getKey().startsWith(SAMPLE_NAME)).isTrue();
        assertThat(response3.getKey()).isNotEqualTo(response2.getKey());
        assertThat((timestamp3 - timestamp2) <= 1000)
            .withFailMessage(
                String.format("wait time for requiring the lock was too long (%dms)", (timestamp3 - timestamp2)))
            .isTrue();

        locksToRelease.add(ByteSequence.from(namespace2.getByteString().concat(response3.getKey().getByteString())));
    }

    private static long grantLease(long ttl) throws Exception {
        CompletableFuture<LeaseGrantResponse> feature = leaseClient.grant(ttl);
        LeaseGrantResponse response = feature.get();
        return response.getID();
    }
}
