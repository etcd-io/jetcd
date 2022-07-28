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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static io.etcd.jetcd.impl.TestUtil.randomByteSequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class WatchErrorTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(3)
        .build();

    @ParameterizedTest
    @ValueSource(strings = { "test-namespace/", "" })
    public void testWatchOnError(String ns) {
        final Client client = ns != null && ns.length() == 0
            ? TestUtil.client(cluster).namespace(bytesOf(ns)).build()
            : TestUtil.client(cluster).build();

        final ByteSequence key = randomByteSequence();
        final List<Throwable> events = Collections.synchronizedList(new ArrayList<>());

        try (Watcher watcher = client.getWatchClient().watch(key, TestUtil::noOpWatchResponseConsumer, events::add)) {
            cluster.cluster().stop();
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> assertThat(events).isNotEmpty());
        }

        assertThat(events).allMatch(EtcdException.class::isInstance);
    }
}
