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

import static org.assertj.core.api.Assertions.assertThat;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Random;
import org.junit.Test;

public class ClientBuilderTest {

  @Test(expected = NullPointerException.class)
  public void testEndPoints_Null() {
    Client.builder().endpoints((String)null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndPoints_Verify_Empty() {
    Client.builder().endpoints("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndPoints_Verify_EmptyAfterTrim() {
    Client.builder().endpoints(" ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndPoints_Verify_SomeEmpty() {
    Client.builder().endpoints("127.0.0.1:2379", " ");
  }

  @Test(expected = IllegalStateException.class)
  public void testBuild_WithoutEndpoints() {
    Client.builder().build();
  }

  @Test
  public void testMaxInboundMessageSize() {
    final int value = 1024 * 1 + new Random().nextInt(10);
    final ClientBuilder builder =  Client.builder().endpoints("http://127.0.0.1:2379").maxInboundMessageSize(value);
    final NettyChannelBuilder channelBuilder = (NettyChannelBuilder)new ClientConnectionManager(builder).defaultChannelBuilder();

    assertThat(channelBuilder).hasFieldOrPropertyWithValue("maxInboundMessageSize", value);
  }
}
