/*
 * Copyright 2016-2020 The jetcd authors
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.etcd.jetcd.auth.Permission;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Timeout(value = 30)
public class WatchTokenExpireTest {
    // create a cluster with SSL enabled, because otherwise volumes with certificates are not mapped
    // to test Docker container. setup JWT authentication provider, it allows to configure short
    // time-to-live of the token.
    @RegisterExtension
    public static final EtcdClusterExtension cluster = new EtcdClusterExtension(
        "etcd-ssl",
        1,
        true,
        "--auth-token",
        "jwt,pub-key=/etc/ssl/etcd/server.pem,priv-key=/etc/ssl/etcd/server-key.pem,sign-method=RS256,ttl=1s");

    private static final ByteSequence key = TestUtil.randomByteSequence();
    private static final ByteSequence user = TestUtil.bytesOf("root");
    private static final ByteSequence password = TestUtil.randomByteSequence();

    private void setUpEnvironment() throws Exception {
        final File caFile = new File(getClass().getResource("/ssl/cert/ca.pem").toURI());

        Client client = Client.builder()
            .endpoints(cluster.getClientEndpoints())
            .authority("etcd0")
            .sslContext(b -> b.trustManager(caFile))
            .build();

        // enable authentication to enforce usage of access token
        ByteSequence role = TestUtil.bytesOf("root");
        client.getAuthClient().roleAdd(role).get();
        client.getAuthClient().userAdd(user, password).get();
        // grant access only to given key
        client.getAuthClient().roleGrantPermission(role, key, key, Permission.Type.READWRITE).get();
        client.getAuthClient().userGrantRole(user, role).get();
        client.getAuthClient().authEnable().get();

        client.close();
    }

    private Client createAuthClient() throws Exception {
        final File caFile = new File(getClass().getResource("/ssl/cert/ca.pem").toURI());

        return Client.builder()
            .endpoints(cluster.getClientEndpoints())
            .user(user)
            .password(password)
            .authority("etcd0")
            .sslContext(b -> b.trustManager(caFile)).build();
    }

    @Test
    public void testRefreshExpiredToken() throws Exception {
        setUpEnvironment();

        Client authClient = createAuthClient();
        Watch authWatchClient = authClient.getWatchClient();
        KV authKVClient = authClient.getKVClient();

        authKVClient.put(key, TestUtil.randomByteSequence()).get(1, TimeUnit.SECONDS);
        Thread.sleep(3000);

        AtomicInteger modifications = new AtomicInteger();

        // watch should handle token refresh automatically
        // token is already expired when we attempt to create a watch
        Watch.Watcher watcher = authWatchClient.watch(key, response -> {
            modifications.incrementAndGet();
        });

        // create single thread pool, so that tasks are executed one after another
        ExecutorService executor = Executors.newFixedThreadPool(1);
        List<Future<?>> futures = new ArrayList<>(2);
        Client anotherClient = createAuthClient();
        for (int i = 0; i < 2; ++i) {
            futures.add(executor.submit(() -> {
                try {
                    // wait 3 seconds for token to expire. during the test token will be refreshed twice
                    Thread.sleep(3000);
                    anotherClient.getKVClient().put(key, TestUtil.randomByteSequence()).get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> assertThat(modifications.get()).isEqualTo(2));

        executor.shutdownNow();
        futures.forEach(f -> assertThat(f).isDone());

        anotherClient.close();
        watcher.close();
        authWatchClient.close();
        authClient.close();
    }
}
