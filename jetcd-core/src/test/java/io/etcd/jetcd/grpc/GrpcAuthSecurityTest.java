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

package io.etcd.jetcd.grpc;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.SecureByteSequence;

import static org.assertj.core.api.Assertions.assertThat;

public class GrpcAuthSecurityTest {

    @Test
    public void testTokenIsStoredAsSecureByteSequence() throws Exception {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        @SuppressWarnings("unused")
        GrpcAuth grpcAuth = grpcService.auth();

        Field tokenField = GrpcAuth.class.getDeclaredField("token");
        tokenField.setAccessible(true);

        assertThat(tokenField.getType()).isEqualTo(SecureByteSequence.class);
    }

    @Test
    public void testRefreshTokenZerosOldToken() throws Exception {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        SecureByteSequence mockToken = SecureByteSequence.from("mock-token");

        Field tokenField = GrpcAuth.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        tokenField.set(grpcAuth, mockToken);

        Field bytesField = SecureByteSequence.class.getDeclaredField("bytes");
        bytesField.setAccessible(true);
        byte[] tokenBytes = (byte[]) bytesField.get(mockToken);

        assertThat(tokenBytes).isNotEqualTo(new byte[tokenBytes.length]);

        grpcAuth.refreshToken();

        assertThat(tokenBytes).isEqualTo(new byte[tokenBytes.length]);
        assertThat(mockToken.isClosed()).isTrue();
    }

    @Test
    public void testCloseZerosToken() throws Exception {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        SecureByteSequence mockToken = SecureByteSequence.from("mock-token-data");

        Field tokenField = GrpcAuth.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        tokenField.set(grpcAuth, mockToken);

        Field bytesField = SecureByteSequence.class.getDeclaredField("bytes");
        bytesField.setAccessible(true);
        byte[] tokenBytes = (byte[]) bytesField.get(mockToken);

        assertThat(tokenBytes).isNotEqualTo(new byte[tokenBytes.length]);

        grpcAuth.close();

        assertThat(tokenBytes).isEqualTo(new byte[tokenBytes.length]);
        assertThat(mockToken.isClosed()).isTrue();

        Object storedToken = tokenField.get(grpcAuth);
        assertThat(storedToken).isNull();
    }

    @Test
    public void testMultipleCloseCallsAreSafe() throws Exception {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        SecureByteSequence mockToken = SecureByteSequence.from("mock-token");

        Field tokenField = GrpcAuth.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        tokenField.set(grpcAuth, mockToken);

        grpcAuth.close();
        grpcAuth.close();
        grpcAuth.close();

        assertThat(mockToken.isClosed()).isTrue();
    }

    @Test
    public void testRefreshTokenWhenTokenIsNull() {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        grpcAuth.refreshToken();
    }

    @Test
    public void testCloseWhenTokenIsNull() {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        grpcAuth.close();
    }

    @Test
    public void testRequiresAuthWithNoCredentials() {
        ClientBuilder builder = Client.builder("http://localhost:2379");
        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        assertThat(grpcAuth.requiresAuth()).isFalse();
    }

    @Test
    public void testRequiresAuthWithCredentials() {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        assertThat(grpcAuth.requiresAuth()).isTrue();
    }

    @Test
    public void testGrpcServiceCloseCallsAuthClose() throws Exception {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        SecureByteSequence mockToken = SecureByteSequence.from("service-token");

        Field tokenField = GrpcAuth.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        tokenField.set(grpcAuth, mockToken);

        Field bytesField = SecureByteSequence.class.getDeclaredField("bytes");
        bytesField.setAccessible(true);
        byte[] tokenBytes = (byte[]) bytesField.get(mockToken);

        assertThat(tokenBytes).isNotEqualTo(new byte[tokenBytes.length]);

        grpcService.close().join();

        assertThat(tokenBytes).isEqualTo(new byte[tokenBytes.length]);
        assertThat(mockToken.isClosed()).isTrue();
    }

    @Test
    public void testGetTokenWithClosedToken() throws Exception {
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(SecureByteSequence.from("admin"))
            .password(SecureByteSequence.from("secret"));

        GrpcService grpcService = new GrpcService(builder);
        GrpcAuth grpcAuth = grpcService.auth();

        SecureByteSequence mockToken = SecureByteSequence.from("closed-token");
        mockToken.close();

        Field tokenField = GrpcAuth.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        tokenField.set(grpcAuth, mockToken);

        assertThat(mockToken.isClosed()).isTrue();
    }
}
