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
package io.etcd.jetcd;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.netty.NettyChannelBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import org.junit.Test;

public class ClientBuilderTest {

  @Test(expected = NullPointerException.class)
  public void testEndPoints_Null() {
    Client.builder().endpoints((URI)null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndPoints_Verify_Empty() throws URISyntaxException {
    Client.builder().endpoints(new URI(""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndPoints_Verify_SomeEmpty() throws URISyntaxException {
    Client.builder().endpoints(new URI("http://127.0.0.1:2379"), new URI(""));
  }

  @Test(expected = IllegalStateException.class)
  public void testBuild_WithoutEndpoints() {
    Client.builder().build();
  }

  @Test
  public void testMaxInboundMessageSize() throws URISyntaxException {
    final int value = 1024 * 1 + new Random().nextInt(10);
    final ClientBuilder builder =  Client.builder().endpoints(new URI("http://127.0.0.1:2379")).maxInboundMessageSize(value);
    final NettyChannelBuilder channelBuilder = (NettyChannelBuilder)new ClientConnectionManager(builder).defaultChannelBuilder();

    assertThat(channelBuilder).hasFieldOrPropertyWithValue("maxInboundMessageSize", value);
  }
}
