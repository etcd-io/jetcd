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

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.etcd.jetcd.watch.WatchResponse;

import com.google.protobuf.ByteString;

public class TestUtil {

    public static ByteSequence bytesOf(final String string) {
        return ByteSequence.from(string, StandardCharsets.UTF_8);
    }

    public static ByteString byteStringOf(final String string) {
        return ByteString.copyFrom(string.getBytes(StandardCharsets.UTF_8));
    }

    public static String randomString() {
        return java.util.UUID.randomUUID().toString();
    }

    public static ByteSequence randomByteSequence() {
        return ByteSequence.from(randomString(), StandardCharsets.UTF_8);
    }

    public static int findNextAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }

    public interface TestCondition {
        boolean conditionMet();
    }

    public static void waitForCondition(final TestCondition testCondition, final long maxWaitMs,
        String conditionDetails) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        boolean testConditionMet = false;
        while (!(testConditionMet = testCondition.conditionMet()) && (System.currentTimeMillis() - startTime) < maxWaitMs) {
            Thread.sleep(Math.min(maxWaitMs, 500L));
        }
        if (!testConditionMet) {
            conditionDetails = conditionDetails != null ? conditionDetails : "";
            throw new AssertionError("Condition not met within timeout " + maxWaitMs + ". " + conditionDetails);
        }
    }

    public static void noOpWatchResponseConsumer(WatchResponse response) {
        // no-op
    }

    public static ClientBuilder client(EtcdClusterExtension extension) {
        return Client.builder().target("cluster://" + extension.clusterName());
    }

    public static ClientBuilder client(EtcdCluster cluster) {
        return Client.builder().target("cluster://" + cluster.clusterName());
    }
}
