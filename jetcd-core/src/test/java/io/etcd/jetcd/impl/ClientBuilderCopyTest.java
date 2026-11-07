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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ClientBuilderCopyTest {
    private static final Metadata.Key<String> HEADER = Metadata.Key.of("x-client", Metadata.ASCII_STRING_MARSHALLER);

    @ParameterizedTest
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void copiesHeadersIndependently(boolean auth, boolean modifyOriginal) {
        ClientBuilder original = Client.builder();
        addHeader(original, auth, "initial");
        ClientBuilder copy = original.copy();
        ClientBuilder changed = modifyOriginal ? original : copy;
        ClientBuilder unchanged = modifyOriginal ? copy : original;

        addHeader(changed, auth, "changed");

        assertThat(headers(unchanged, auth)).containsEntry(HEADER, "initial");
        assertThat(headers(changed, auth)).containsEntry(HEADER, "changed");
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void copiesInterceptorsIndependently(boolean auth, boolean modifyOriginal) {
        ClientInterceptor first = MetadataUtils.newAttachHeadersInterceptor(new Metadata());
        ClientInterceptor second = MetadataUtils.newAttachHeadersInterceptor(new Metadata());
        ClientBuilder original = Client.builder();
        addInterceptor(original, auth, first);
        ClientBuilder copy = original.copy();
        ClientBuilder changed = modifyOriginal ? original : copy;
        ClientBuilder unchanged = modifyOriginal ? copy : original;

        addInterceptor(changed, auth, second);

        assertThat(interceptors(unchanged, auth)).containsExactly(first);
        assertThat(interceptors(changed, auth)).containsExactly(first, second);
    }

    @Test
    void copyRetainsHeaderValuesAndInterceptorInstances() {
        Metadata.Key<byte[]> key = Metadata.Key.of("x-data-bin", Metadata.BINARY_BYTE_MARSHALLER);
        byte[] value = { 1, 2 };
        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(new Metadata());
        ClientBuilder original = Client.builder()
            .headers(Map.of(key, value))
            .authHeaders(Map.of(key, value))
            .interceptor(interceptor)
            .authInterceptors(interceptor);

        ClientBuilder copy = original.copy();

        assertThat(copy.headers().get(key)).isSameAs(value);
        assertThat(copy.authHeaders().get(key)).isSameAs(value);
        assertThat(copy.interceptors().get(0)).isSameAs(interceptor);
        assertThat(copy.authInterceptors().get(0)).isSameAs(interceptor);
    }

    @Test
    void copiesDefaultBuilderWithoutInitializingCollections() {
        ClientBuilder original = Client.builder();
        ClientBuilder copy = original.copy();

        assertThat(copy.headers()).isEmpty();
        assertThat(copy.authHeaders()).isEmpty();
        assertThat(copy.interceptors()).isNull();
        assertThat(copy.authInterceptors()).isNull();

        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(new Metadata());
        copy.header("x-client", "copy").authHeader("x-client", "copy")
            .interceptor(interceptor).authInterceptors(interceptor);

        assertThat(original.headers()).isEmpty();
        assertThat(original.authHeaders()).isEmpty();
        assertThat(original.interceptors()).isNull();
        assertThat(original.authInterceptors()).isNull();
    }

    private static void addHeader(ClientBuilder builder, boolean auth, String value) {
        if (auth) {
            builder.authHeader("x-client", value);
        } else {
            builder.header("x-client", value);
        }
    }

    private static Map<Metadata.Key<?>, Object> headers(ClientBuilder builder, boolean auth) {
        return auth ? builder.authHeaders() : builder.headers();
    }

    private static void addInterceptor(ClientBuilder builder, boolean auth, ClientInterceptor interceptor) {
        if (auth) {
            builder.authInterceptors(interceptor);
        } else {
            builder.interceptor(interceptor);
        }
    }

    private static List<ClientInterceptor> interceptors(ClientBuilder builder, boolean auth) {
        return auth ? builder.authInterceptors() : builder.interceptors();
    }
}
