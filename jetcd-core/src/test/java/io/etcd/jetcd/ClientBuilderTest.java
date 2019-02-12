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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.Charsets;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.stream.Stream;


public class ClientBuilderTest {

  @Test
  public void testEndPoints_Null() {
    assertThrows(NullPointerException.class, () -> Client.builder().endpoints((URI)null));
  }

  @Test
  public void testEndPoints_Verify_Empty() throws URISyntaxException {
    assertThrows(IllegalArgumentException.class, () -> Client.builder().endpoints(new URI("")));
  }

  @Test
  public void testEndPoints_Verify_SomeEmpty() throws URISyntaxException {
    assertThrows(IllegalArgumentException.class,
        () -> Client.builder().endpoints(new URI("http://127.0.0.1:2379"), new URI("")));
  }

  @Test
  public void testBuild_WithoutEndpoints() {
    assertThrows(IllegalStateException.class, () -> Client.builder().build());
  }

  @Test
  public void testMaxInboundMessageSize() throws URISyntaxException {
    final int value = 1024 * 1 + new Random().nextInt(10);
    final ClientBuilder builder =  Client.builder().endpoints(new URI("http://127.0.0.1:2379")).maxInboundMessageSize(value);
    final NettyChannelBuilder channelBuilder = (NettyChannelBuilder)new ClientConnectionManager(builder).defaultChannelBuilder();

    assertThat(channelBuilder).hasFieldOrPropertyWithValue("maxInboundMessageSize", value);
  }

  @Test
  public void testDefaultNamespace() throws URISyntaxException {
    // test default namespace setting
    final ClientBuilder builder =  Client.builder().endpoints(new URI("http://127.0.0.1:2379"));
    final ClientConnectionManager connectionManager = new ClientConnectionManager(builder);
    assertThat(connectionManager.getNamespace()).isEqualTo(ByteSequence.EMPTY);
  }

  static Stream<Arguments> namespaceProvider() {
    return Stream.of(
        // namespace setting, expected namespace
        Arguments.of(ByteSequence.EMPTY, ByteSequence.EMPTY),
        Arguments.of(ByteSequence.from("/namespace1/", Charsets.UTF_8),
            ByteSequence.from("/namespace1/", Charsets.UTF_8)),
        Arguments.of(ByteSequence.from("namespace2/", Charsets.UTF_8),
            ByteSequence.from("namespace2/", Charsets.UTF_8))
    );
  }

  @ParameterizedTest
  @MethodSource("namespaceProvider")
  public void testNamespace(ByteSequence namespaceSetting, ByteSequence expectedNamespace)
      throws URISyntaxException {
      final ClientBuilder builder =  Client.builder().endpoints(new URI("http://127.0.0.1:2379"))
          .namespace(namespaceSetting);
      final ClientConnectionManager connectionManager = new ClientConnectionManager(builder);
      assertThat(connectionManager.getNamespace()).isEqualTo(expectedNamespace);
  }

}
