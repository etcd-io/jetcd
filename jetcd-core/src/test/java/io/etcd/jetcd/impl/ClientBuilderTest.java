/*
 * Copyright 2016-2021 The jetcd authors
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

package io.etcd.jetcd.impl;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.grpc.GrpcService;
import io.etcd.jetcd.resolver.ServiceResolver;
import io.etcd.jetcd.resolver.ServiceResolvers;
import io.vertx.core.net.endpoint.LoadBalancer;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
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
        assertThatThrownBy(() -> Client.builder((String) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyKeepaliveTimeoutRemainsAnAliasForLeaseKeepAliveTimeout() {
        var builder = Client.builder("http://localhost:2379");
        assertThat(builder.leaseKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(builder.keepaliveTimeout(Duration.ofSeconds(3)).leaseKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(builder.leaseKeepAliveTimeout(null).keepaliveTimeout()).isNull();
    }

    @Test
    public void testVertx_Null() {
        assertThatThrownBy(() -> Client.builder("http://127.0.0.1:2379").vertx(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testEndPoints_Verify_Empty() {
        assertThatThrownBy(() -> Client.builder("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testEndPoints_Verify_SomeEmpty() {
        assertThatThrownBy(() -> Client.builder("http://127.0.0.1:2379", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDefaultNamespace() throws URISyntaxException {
        // test default namespace setting
        final ClientBuilder builder = Client.builder("http://127.0.0.1:2379");
        final GrpcService grpcService = new GrpcService(builder);
        assertThat(grpcService.getNamespace()).isEqualTo(ByteSequence.EMPTY);
    }

    @ParameterizedTest
    @MethodSource("namespaceProvider")
    public void testNamespace(ByteSequence namespaceSetting, ByteSequence expectedNamespace) throws URISyntaxException {
        final ClientBuilder builder = Client.builder("http://127.0.0.1:2379").namespace(namespaceSetting);
        final GrpcService grpcService = new GrpcService(builder);
        assertThat(grpcService.getNamespace()).isEqualTo(expectedNamespace);
    }

    @Test
    public void testEndpointResolvers_DnsSrv() {
        // Test creating a DNS SRV resolver with default DNS server
        ServiceResolver resolver = ServiceResolvers.dnsSrv("_etcd._tcp.example.com");

        assertThat(resolver).isNotNull();
        assertThat(resolver.getResolver()).isNotNull();
        assertThat(resolver.getTarget()).isNotNull();
    }

    @Test
    public void testEndpointResolvers_DnsSrvWithCustomDns() {
        // Test creating a DNS SRV resolver with custom DNS server
        ServiceResolver resolver = ServiceResolvers.dnsSrv("_etcd._tcp.example.com", "dns.example.com", 53);

        assertThat(resolver).isNotNull();
        assertThat(resolver.getResolver()).isNotNull();
        assertThat(resolver.getTarget()).isNotNull();
    }

    @Test
    public void testEndpointResolvers_Endpoints() throws URISyntaxException {
        // Test creating a static endpoint resolver
        ServiceResolver resolver = ServiceResolvers.endpoints("http://127.0.0.1:2379", "http://127.0.0.1:2380");

        assertThat(resolver).isNotNull();
        assertThat(resolver.getResolver()).isNotNull();
        assertThat(resolver.getTarget()).isNotNull();
    }

    @Test
    public void testClient_BuilderWithDnsSrvResolver() {
        // Test creating client with DNS SRV resolver
        ClientBuilder builder = Client.builder(ServiceResolvers.dnsSrv("_etcd._tcp.example.com"));

        assertThat(builder).isNotNull();
        assertThat(builder.serviceResolver()).isNotNull();
        assertThat(builder.serviceResolver().getResolver()).isNotNull();
        assertThat(builder.serviceResolver().getTarget()).isNotNull();
    }

    @Test
    public void testLoadBalancer_RoundRobin() throws URISyntaxException {
        ClientBuilder builder = Client.builder("http://127.0.0.1:2379").loadBalancer(LoadBalancer.ROUND_ROBIN);

        assertThat(builder.loadBalancer()).isEqualTo(LoadBalancer.ROUND_ROBIN);
    }

    @Test
    public void testLoadBalancer_LeastRequests() throws URISyntaxException {
        ClientBuilder builder = Client.builder("http://127.0.0.1:2379").loadBalancer(LoadBalancer.LEAST_REQUESTS);

        assertThat(builder.loadBalancer()).isEqualTo(LoadBalancer.LEAST_REQUESTS);
    }

    @Test
    public void testLoadBalancer_Random() throws URISyntaxException {
        ClientBuilder builder = Client.builder("http://127.0.0.1:2379").loadBalancer(LoadBalancer.RANDOM);

        assertThat(builder.loadBalancer()).isEqualTo(LoadBalancer.RANDOM);
    }

    @Test
    public void testLoadBalancer_PowerOfTwoChoices() throws URISyntaxException {
        ClientBuilder builder = Client.builder("http://127.0.0.1:2379").loadBalancer(LoadBalancer.POWER_OF_TWO_CHOICES);

        assertThat(builder.loadBalancer()).isEqualTo(LoadBalancer.POWER_OF_TWO_CHOICES);
    }

    @Test
    public void testLoadBalancer_DefaultNull() throws URISyntaxException {
        ClientBuilder builder = Client.builder("http://127.0.0.1:2379");

        // Verify default is null when not specified (GrpcService will default to ROUND_ROBIN)
        assertThat(builder.loadBalancer()).isNull();
    }

}
