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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.google.common.base.Charsets;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.launcher.junit.EtcdClusterResource;
import io.grpc.netty.GrpcSslContexts;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.junit.Rule;
import org.junit.Test;

public class SslTest {

  @Rule
  public final EtcdClusterResource clusterResource = new EtcdClusterResource("etcd-ssl", 1, true);

  private static final String DEFAULT_SSL_AUTHORITY = "etcd0";
  private static final String DEFAULT_SSL_CA_PATH = "/ssl/cert/ca.pem";

  @Test(timeout = 5000)
  public void testSimpleSllSetup() throws Exception {
    final ByteSequence key = ByteSequence.from(TestUtil.randomString(), Charsets.UTF_8);
    final ByteSequence val = ByteSequence.from(TestUtil.randomString(), Charsets.UTF_8);
    final String capath = System.getProperty("ssl.cert.capath");
    final String authority = System.getProperty("ssl.cert.authority", DEFAULT_SSL_AUTHORITY);
    final URI endpoint = new URI(System.getProperty("ssl.cert.endpoints", clusterResource.cluster().getClientEndpoints().get(0).toString()));

    try (InputStream is = Objects.nonNull(capath)
          ? new FileInputStream(new File(capath))
          : getClass().getResourceAsStream(DEFAULT_SSL_CA_PATH)) {

      Client client = Client.builder()
          .endpoints(endpoint)
          .authority(authority)
          .sslContext(GrpcSslContexts.forClient()
              .trustManager(is)
              .build())
          .build();

      KV kv = client.getKVClient();
      kv.put(key, val).join();

      assertThat(kv.get(key).join().getCount()).isEqualTo(1);
      assertThat(kv.get(key).join().getKvs().get(0).getValue()).isEqualTo(val);

      kv.close();
      client.close();
    }
  }
}

