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

package io.etcd.jetcd.launcher.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;

/**
 * Simple test illustrating how to use the {@link EtcdClusterFactory} with the jetcd client.
 *
 * <p>Tests typically would use the EtcdClusterResource JUnit rule instead of the EtcdClusterFactory direct.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdClusterUsingTest {

  @Test
  public void testUseEtcd() throws Exception {
    try (EtcdCluster etcd = EtcdClusterFactory.buildCluster(getClass().getSimpleName(), 3, false, false)) {
      etcd.start();
      try (Client client = Client.builder().endpoints(etcd.getClientEndpoints()).build()) {
        try (KV kvClient = client.getKVClient()) {

          ByteSequence key = ByteSequence.from("test_key", UTF_8);
          ByteSequence value = ByteSequence.from("test_value", UTF_8);
          kvClient.put(key, value).get();

          CompletableFuture<GetResponse> getFuture = kvClient.get(key);
          GetResponse response = getFuture.get();
          List<KeyValue> values = response.getKvs();
          assertEquals(1, values.size());
          KeyValue value1 = values.get(0);
          assertEquals(value, value1.getValue());
          assertEquals(key, value1.getKey());
        }
      }
    }
  }
}
