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
import java.net.URISyntaxException;
import java.util.Random;
import java.util.stream.Stream;

import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.etcd.jetcd.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClientBuilderTest {

    static Stream<Arguments> namespaceProvider() {
        return Stream.of(
            // namespace setting, expected namespace
            Arguments.of(ByteSequence.EMPTY, ByteSequence.EMPTY),
            Arguments.of(bytesOf("/namespace1/"), bytesOf("/namespace1/")),
            Arguments.of(bytesOf("namespace2/"), bytesOf("namespace2/")));
    }

    @Test
    public void testEndPoints_Null() {
        assertThatThrownBy(() -> Client.builder().endpoints((URI) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testEndPoints_Verify_Empty() {
        assertThatThrownBy(() -> Client.builder().endpoints(new URI(""))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testEndPoints_Verify_SomeEmpty() {
        assertThatThrownBy(() -> Client.builder().endpoints(new URI("http://127.0.0.1:2379"), new URI("")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBuild_WithoutEndpoints() {
        assertThatThrownBy(() -> Client.builder().build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testMaxInboundMessageSize() throws URISyntaxException {
        final int value = 1024 + new Random().nextInt(10);
        final ClientBuilder builder = Client.builder().endpoints(new URI("http://127.0.0.1:2379")).maxInboundMessageSize(value);
        final NettyChannelBuilder channelBuilder = (NettyChannelBuilder) new ClientConnectionManager(builder)
            .defaultChannelBuilder();

        assertThat(channelBuilder).hasFieldOrPropertyWithValue("maxInboundMessageSize", value);
    }

    @Test
    public void testDefaultNamespace() throws URISyntaxException {
        // test default namespace setting
        final ClientBuilder builder = Client.builder().endpoints(new URI("http://127.0.0.1:2379"));
        final ClientConnectionManager connectionManager = new ClientConnectionManager(builder);
        assertThat(connectionManager.getNamespace()).isEqualTo(ByteSequence.EMPTY);
    }

    @ParameterizedTest
    @MethodSource("namespaceProvider")
    public void testNamespace(ByteSequence namespaceSetting, ByteSequence expectedNamespace) throws URISyntaxException {
        final ClientBuilder builder = Client.builder().endpoints(new URI("http://127.0.0.1:2379")).namespace(namespaceSetting);
        final ClientConnectionManager connectionManager = new ClientConnectionManager(builder);
        assertThat(connectionManager.getNamespace()).isEqualTo(expectedNamespace);
    }

}
