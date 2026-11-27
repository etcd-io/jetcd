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

package io.etcd.jetcd;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientBuilderSecurityTest {

    @Test
    public void testSecureByteSequenceUserIsStored() {
        SecureByteSequence user = SecureByteSequence.from("admin");
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(user);

        assertThat(builder.user()).isNotNull();
        assertThat(builder.user().getBytes()).isEqualTo("admin".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testSecureByteSequencePasswordIsStored() {
        SecureByteSequence password = SecureByteSequence.from("secret");
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .password(password);

        assertThat(builder.password()).isNotNull();
        assertThat(builder.password().getBytes()).isEqualTo("secret".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testPasswordZeroingAfterClose() throws Exception {
        byte[] passwordBytes = "secret123".getBytes(StandardCharsets.UTF_8);
        SecureByteSequence password = SecureByteSequence.from(passwordBytes);

        @SuppressWarnings("unused")
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .password(password);

        Field bytesField = SecureByteSequence.class.getDeclaredField("bytes");
        bytesField.setAccessible(true);
        byte[] internalBytes = (byte[]) bytesField.get(password);

        assertThat(internalBytes).isNotEqualTo(new byte[internalBytes.length]);

        password.close();

        assertThat(internalBytes).isEqualTo(new byte[internalBytes.length]);
    }

    @Test
    public void testUserZeroingAfterClose() throws Exception {
        byte[] userBytes = "admin".getBytes(StandardCharsets.UTF_8);
        SecureByteSequence user = SecureByteSequence.from(userBytes);

        @SuppressWarnings("unused")
        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(user);

        Field bytesField = SecureByteSequence.class.getDeclaredField("bytes");
        bytesField.setAccessible(true);
        byte[] internalBytes = (byte[]) bytesField.get(user);

        assertThat(internalBytes).isNotEqualTo(new byte[internalBytes.length]);

        user.close();

        assertThat(internalBytes).isEqualTo(new byte[internalBytes.length]);
    }

    @Test
    public void testMultiplePasswordCallsWithSecureByteSequence() throws Exception {
        SecureByteSequence password1 = SecureByteSequence.from("password1");
        SecureByteSequence password2 = SecureByteSequence.from("password2");

        ClientBuilder builder = Client.builder("http://localhost:2379")
            .password(password1)
            .password(password2);

        Field passwordField = ClientBuilder.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        SecureByteSequence storedPassword = (SecureByteSequence) passwordField.get(builder);

        assertThat(storedPassword).isSameAs(password2);
        assertThat(builder.password().getBytes()).isEqualTo("password2".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testBuilderCopy() {
        SecureByteSequence user = SecureByteSequence.from("admin");
        SecureByteSequence password = SecureByteSequence.from("secret");

        ClientBuilder builder = Client.builder("http://localhost:2379")
            .user(user)
            .password(password);

        ClientBuilder copy = builder.copy();

        assertThat(copy.user()).isNotNull();
        assertThat(copy.user().getBytes()).isEqualTo("admin".getBytes(StandardCharsets.UTF_8));
        assertThat(copy.password()).isNotNull();
        assertThat(copy.password().getBytes()).isEqualTo("secret".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testNullUserThrowsException() {
        ClientBuilder builder = Client.builder("http://localhost:2379");

        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
            builder.user((SecureByteSequence) null);
        });
    }

    @Test
    public void testNullPasswordThrowsException() {
        ClientBuilder builder = Client.builder("http://localhost:2379");

        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
            builder.password((SecureByteSequence) null);
        });
    }
}
