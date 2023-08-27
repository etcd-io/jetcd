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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.etcd.jetcd.watch.WatchEvent.EventType;
import io.etcd.jetcd.watch.WatchResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Timeout(value = 180, unit = TimeUnit.SECONDS)
public class WatchResumeTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(3)
        .build();

    @ParameterizedTest
    @ValueSource(ints = { 0, 10, 30, 50, 60 })
    public void testWatchOnPut(int delaySec) throws Exception {
        try (Client client = TestUtil.client(cluster).build()) {
            Watch watchClient = client.getWatchClient();
            KV kvClient = client.getKVClient();

            final ByteSequence key = TestUtil.randomByteSequence();
            final ByteSequence value = TestUtil.randomByteSequence();
            final AtomicReference<WatchResponse> ref = new AtomicReference<>();

            try (Watcher watcher = watchClient.watch(key, ref::set)) {
                cluster.restart(delaySec, TimeUnit.SECONDS);

                kvClient.put(key, value).get();

                await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> assertThat(ref.get()).isNotNull());

                assertThat(ref.get().getEvents().size()).isEqualTo(1);
                assertThat(ref.get().getEvents().get(0).getEventType()).isEqualTo(EventType.PUT);
                assertThat(ref.get().getEvents().get(0).getKeyValue().getKey()).isEqualTo(key);
            }
        }
    }
}
