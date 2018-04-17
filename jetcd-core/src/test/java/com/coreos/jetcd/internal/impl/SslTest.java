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

package com.coreos.jetcd.internal.impl;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.internal.infrastructure.ClusterFactory;
import com.coreos.jetcd.internal.infrastructure.EtcdCluster;
import io.grpc.netty.GrpcSslContexts;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.AfterClass;
import org.junit.Test;

public class SslTest {
  private static final EtcdCluster CLUSTER = ClusterFactory.buildSingleNodeClusterWithSsl("etcd-ssl");

  @Test(timeout = 5000)
  public void testSimpleSllSetup() throws Exception {
    final ByteSequence key = ByteSequence.fromString(TestUtil.randomString());
    final ByteSequence val = ByteSequence.fromString(TestUtil.randomString());
    final String capath = System.getProperty("ssl.cert.capath");
    final String authority = System.getProperty("ssl.cert.authority", TestConstants.DEFAULT_SSL_AUTHORITY);
    final String endpoints = System.getProperty("ssl.cert.endpoints", CLUSTER.getClientEndpoints().get(0));

    try (InputStream is = Objects.nonNull(capath)
          ? new FileInputStream(new File(capath))
          : getClass().getResourceAsStream(TestConstants.DEFAULT_SSL_CA_PATH)) {

      Client client = Client.builder()
          .endpoints(endpoints)
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

  @AfterClass
  public static void tearDown() throws IOException {
    CLUSTER.close();
  }
}

