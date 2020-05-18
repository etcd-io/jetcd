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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.etcd.jetcd.TestUtil.bytesOf;
import static io.etcd.jetcd.TestUtil.randomByteSequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Timeout(value = 30)
public class WatchErrorTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = new EtcdClusterExtension("watch", 3);
    public static final ByteSequence namespace = bytesOf("test-namespace/");

    static Stream<Arguments> parameters() {
        return Stream.of(arguments(Client.builder().endpoints(cluster.getClientEndpoints()).namespace(namespace).build()),
            arguments(Client.builder().endpoints(cluster.getClientEndpoints()).build()));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWatchOnError(final Client client) throws Exception {
        final ByteSequence key = randomByteSequence();
        final List<Throwable> events = Collections.synchronizedList(new ArrayList<>());

        try (Watcher watcher = client.getWatchClient().watch(key, TestUtil::noOpWatchResponseConsumer, events::add)) {
            cluster.close();
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> assertThat(events).isNotEmpty());
        }

        assertThat(events).allMatch(EtcdException.class::isInstance);
    }
}
