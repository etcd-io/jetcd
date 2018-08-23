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
package io.etcd.jetcd.internal.impl;


import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.internal.infrastructure.EtcdClusterResource;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;

public class ClientConnectionManagerTest {
  @Rule
  public final EtcdClusterResource clusterResource = new EtcdClusterResource("connection-manager-etcd", 1);

  @Test
  public void test() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    final ClientBuilder builder = Client.builder()
      .endpoints(clusterResource.cluster().getClientEndpoints())
      .header("MyHeader1", "MyHeaderVal1")
      .header("MyHeader2", "MyHeaderVal2")
      .interceptor(new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
          return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
              super.start(responseListener, headers);
              assertThat(headers.get(Metadata.Key.of("MyHeader1", Metadata.ASCII_STRING_MARSHALLER))).isEqualTo("MyHeaderVal1");
              assertThat(headers.get(Metadata.Key.of("MyHeader2", Metadata.ASCII_STRING_MARSHALLER))).isEqualTo("MyHeaderVal2");

              latch.countDown();
            }
          };
        }
      });

    try (Client client = builder.build()) {
      client.getKVClient().put(ByteSequence.from("sample_key", Charsets.UTF_8), ByteSequence.from("sample_key", Charsets.UTF_8));
      latch.await(1, TimeUnit.MINUTES);
    }
  }
}
