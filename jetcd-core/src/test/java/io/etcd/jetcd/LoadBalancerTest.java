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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

// TODO(#548): Add global timeout for tests once JUnit5 supports it
public class LoadBalancerTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = new EtcdClusterExtension("load-balancer-etcd", 3);

    @Test
    public void testPickFirstBalancerFactory() throws Exception {
        final List<URI> endpoints = cluster.getClientEndpoints();
        final ClientBuilder builder = Client.builder().endpoints(endpoints).loadBalancerPolicy("pick_first");

        try (Client client = builder.build();
            KV kv = client.getKVClient()) {

            long lastMemberId = 0;

            final String allEndpoints = endpoints.stream().map(URI::toString).collect(Collectors.joining(","));
            for (int i = 0; i < allEndpoints.length() * 2; i++) {
                Response response = kv.put(TestUtil.randomByteSequence(), TestUtil.randomByteSequence()).get();

                if (i == 0) {
                    lastMemberId = response.getHeader().getMemberId();
                }

                assertThat(response.getHeader().getMemberId()).isEqualTo(lastMemberId);
            }
        }
    }

    @Test
    public void testRoundRobinLoadBalancerFactory() throws Exception {
        final List<URI> endpoints = cluster.getClientEndpoints();
        final ClientBuilder builder = Client.builder().endpoints(endpoints).loadBalancerPolicy("round_robin");

        try (Client client = builder.build();
            KV kv = client.getKVClient()) {

            long lastMemberId = 0;
            long differences = 0;

            final String allEndpoints = endpoints.stream().map(URI::toString).collect(Collectors.joining(","));
            for (int i = 0; i < allEndpoints.length(); i++) {
                PutResponse response = kv.put(TestUtil.randomByteSequence(), TestUtil.randomByteSequence()).get();

                if (i > 0 && lastMemberId != response.getHeader().getMemberId()) {
                    differences++;
                }

                lastMemberId = response.getHeader().getMemberId();
            }

            assertThat(differences).isNotEqualTo(lastMemberId);
        }
    }
}
