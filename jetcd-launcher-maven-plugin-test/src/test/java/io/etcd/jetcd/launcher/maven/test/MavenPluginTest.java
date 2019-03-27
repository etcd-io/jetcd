/*
 * Copyright 2016-2019 The jetcd authors
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

package io.etcd.jetcd.launcher.maven.test;

import static com.google.common.base.Charsets.US_ASCII;
import static org.junit.Assert.assertEquals;

import com.google.common.io.Files;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Constants;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Test etcd server started by jetcd-launcher-maven-plugin.
 *
 * @author Michael Vorburger.ch
 */
public class MavenPluginTest {

  @Test
  public void testEtcdServerStarted() throws Exception {
    final String filePath = "target/jetcd-launcher-maven-plugin/endpoint";
    String endpoint = Files.readFirstLine(new File(filePath), US_ASCII);
    try (Client client = Client.builder().endpoints(endpoint).build()) {
      assertEquals(0, client.getKVClient().get(Constants.NULL_KEY).get(7, TimeUnit.SECONDS).getCount());
    }
  }
}
