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

package io.etcd.jetcd.impl;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Timeout(value = 2, unit = TimeUnit.MINUTES)
public class LockTest {
    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    private Client client;
    private Lock lockClient;
    private static Client staticClient;
    private static Lease leaseClient;
    private Set<ByteSequence> locksToRelease;

    private ByteSequence lockName;

    private static final long RPC_TIMEOUT = 10;
    private static final long LONG_LEASE_TTL = 60;
    private static final ByteSequence NAMESPACE = ByteSequence.from("test-ns/", StandardCharsets.UTF_8);
    private static final ByteSequence SECOND_NAMESPACE = ByteSequence.from("test-ns2/", StandardCharsets.UTF_8);

    @BeforeAll
    public static void setUp() {
        staticClient = TestUtil.client(cluster).build();
        leaseClient = staticClient.getLeaseClient();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (staticClient != null) {
            staticClient.close();
        }
    }

    @BeforeEach
    public void setUpEach() {
        locksToRelease = new HashSet<>();
        lockName = TestUtil.randomByteSequence();
    }

    @AfterEach
    public void tearDownEach() throws Exception {
        if (client == null) {
            return;
        }
        try (Client ownedClient = client) {
            for (ByteSequence lockKey : locksToRelease) {
                ownedClient.getLockClient().unlock(lockKey).get(RPC_TIMEOUT, TimeUnit.SECONDS);
            }
        }
    }

    private void initializeLockClient(boolean useNamespace) {
        client = useNamespace
            ? TestUtil.client(cluster).namespace(NAMESPACE).build()
            : TestUtil.client(cluster).build();
        lockClient = client.getLockClient();
    }

    static Stream<Arguments> parameters() {
        return Stream.of(Arguments.of(true), Arguments.of(false));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLockWithoutLease(boolean useNamespace) throws Exception {
        initializeLockClient(useNamespace);
        var response = lockClient.lock(lockName, 0).get(RPC_TIMEOUT, TimeUnit.SECONDS);
        locksToRelease.add(response.getKey());

        assertThat(response.getHeader()).isNotNull();
        assertThat(response.getKey()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLockWithNotExistingLease(boolean useNamespace) {
        Throwable exception = assertThrows(ExecutionException.class, () -> {
            initializeLockClient(useNamespace);
            var response = lockClient.lock(lockName, 123456).get(RPC_TIMEOUT, TimeUnit.SECONDS);
            locksToRelease.add(response.getKey());
        });
        // With Vert.x gRPC client, error messages are wrapped in InvalidStatusException
        // The actual etcd error about lease not found is in the cause chain
        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause().getMessage()).containsAnyOf("requested lease not found", "UNKNOWN");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLockWithLease(boolean useNamespace) throws Exception {
        initializeLockClient(useNamespace);
        long lease = grantLease(5);
        var holder = lockClient.lock(lockName, lease).get(RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
        locksToRelease.add(holder);

        assertThat(client.getKVClient().get(holder).get(RPC_TIMEOUT, TimeUnit.SECONDS).getCount()).isEqualTo(1);
        var next = lockClient.lock(lockName, 0).get(2 * RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
        locksToRelease.add(next);

        assertThat(next).isNotEqualTo(holder);
        assertThat(client.getKVClient().get(holder).get(RPC_TIMEOUT, TimeUnit.SECONDS).getCount()).isZero();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLockAndUnlock(boolean useNamespace) throws Exception {
        initializeLockClient(useNamespace);
        long lease = grantLease(LONG_LEASE_TTL);
        var holder = lockClient.lock(lockName, lease).get(RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
        locksToRelease.add(holder);
        var contender = lockClient.lock(lockName, 0);

        var prefix = lockName.concat(ByteSequence.NAMESPACE_DELIMITER);
        var range = GetOption.builder().isPrefix(true).build();
        await().atMost(RPC_TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            var queued = client.getKVClient().get(prefix, range).get(RPC_TIMEOUT, TimeUnit.SECONDS);
            assertThat(queued.getCount()).isEqualTo(2);
        });
        assertThat(contender).isNotDone();

        lockClient.unlock(holder).get(RPC_TIMEOUT, TimeUnit.SECONDS);
        locksToRelease.remove(holder);
        var next = contender.get(RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
        locksToRelease.add(next);

        assertThat(next).isNotEqualTo(holder);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testCancelQueuedLock(boolean useNamespace) throws Exception {
        initializeLockClient(useNamespace);
        long holderLease = grantLease(LONG_LEASE_TTL);
        var holder = lockClient.lock(lockName, holderLease).get(RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
        locksToRelease.add(holder);

        long contenderLease = grantLease(LONG_LEASE_TTL);
        try {
            var contender = lockClient.lock(lockName, contenderLease);
            var prefix = lockName.concat(ByteSequence.NAMESPACE_DELIMITER);
            var range = GetOption.builder().isPrefix(true).build();
            await().atMost(RPC_TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
                var queued = client.getKVClient().get(prefix, range).get(RPC_TIMEOUT, TimeUnit.SECONDS);
                assertThat(queued.getKvs()).extracting(KeyValue::getLease).containsExactlyInAnyOrder(holderLease,
                    contenderLease);
            });

            assertThat(contender.cancel(false)).isTrue();
            assertThat(contender).isCancelled();
            await().atMost(RPC_TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
                var queued = client.getKVClient().get(prefix, range).get(RPC_TIMEOUT, TimeUnit.SECONDS);
                assertThat(queued.getKvs()).extracting(KeyValue::getKey).containsExactly(holder);
            });
        } finally {
            leaseClient.revoke(contenderLease).get(RPC_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testLockSegregationByNamespaces() throws Exception {
        initializeLockClient(false);

        try (Client firstClient = TestUtil.client(cluster).namespace(NAMESPACE).build();
            Client secondClient = TestUtil.client(cluster).namespace(SECOND_NAMESPACE).build()) {
            var firstLock = firstClient.getLockClient();
            var firstKey = firstLock.lock(lockName, grantLease(LONG_LEASE_TTL))
                .get(RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
            var fullKey = NAMESPACE.concat(firstKey);
            locksToRelease.add(fullKey);
            assertThat(firstKey.startsWith(lockName)).isTrue();

            lockClient.unlock(fullKey).get(RPC_TIMEOUT, TimeUnit.SECONDS);
            locksToRelease.remove(fullKey);
            var reacquired = firstLock.lock(lockName, grantLease(LONG_LEASE_TTL))
                .get(RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
            locksToRelease.add(NAMESPACE.concat(reacquired));
            assertThat(reacquired).isNotEqualTo(firstKey);

            var secondKey = secondClient.getLockClient().lock(lockName, grantLease(LONG_LEASE_TTL))
                .get(RPC_TIMEOUT, TimeUnit.SECONDS).getKey();
            locksToRelease.add(SECOND_NAMESPACE.concat(secondKey));

            assertThat(secondKey.startsWith(lockName)).isTrue();
            assertThat(secondKey).isNotEqualTo(reacquired);
            assertThat(firstClient.getKVClient().get(reacquired).get(RPC_TIMEOUT, TimeUnit.SECONDS).getCount())
                .isEqualTo(1);
        }
    }

    private static long grantLease(long ttl) throws Exception {
        return leaseClient.grant(ttl).get(RPC_TIMEOUT, TimeUnit.SECONDS).getID();
    }
}
