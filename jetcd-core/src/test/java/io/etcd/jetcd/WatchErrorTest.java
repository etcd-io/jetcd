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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;

import static io.etcd.jetcd.TestUtil.bytesOf;
import static io.etcd.jetcd.TestUtil.randomByteSequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Timeout(value = 30)
public class WatchErrorTest {
    @ParameterizedTest
    @ValueSource(strings = { "test-namespace/", "" })
    public void testWatchOnError(String ns) {
        try (EtcdCluster cluster = EtcdClusterFactory.buildCluster("watch", 3, false)) {
            cluster.start();

            final Client client = ns != null && ns.length() == 0
                ? Client.builder().endpoints(cluster.getClientEndpoints()).namespace(bytesOf(ns)).build()
                : Client.builder().endpoints(cluster.getClientEndpoints()).build();

            final ByteSequence key = randomByteSequence();
            final List<Throwable> events = Collections.synchronizedList(new ArrayList<>());

            try (Watcher watcher = client.getWatchClient().watch(key, TestUtil::noOpWatchResponseConsumer, events::add)) {
                cluster.close();
                await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> assertThat(events).isNotEmpty());
            }

            assertThat(events).allMatch(EtcdException.class::isInstance);
        }
    }
}
