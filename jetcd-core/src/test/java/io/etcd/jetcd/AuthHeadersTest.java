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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.auth.Permission;
import io.etcd.jetcd.support.Interceptors;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.grpc.Metadata;

import static io.etcd.jetcd.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 1, unit = TimeUnit.MINUTES)
public class AuthHeadersTest {
    @RegisterExtension
    public static final EtcdClusterExtension cluster = new EtcdClusterExtension("auth-etcd", 1, false);

    private final ByteSequence rootRoleKeyRangeBegin = bytesOf("root");
    private final ByteSequence rootRoleKeyRangeEnd = bytesOf("root1");

    private final ByteSequence userRoleKey = bytesOf("foo");
    private final ByteSequence userRoleValue = bytesOf("bar");
    private final ByteSequence userRoleKeyRangeBegin = bytesOf("foo");
    private final ByteSequence userRoleKeyRangeEnd = bytesOf("foo1");

    private final String rootString = "root";
    private final ByteSequence root = bytesOf(rootString);
    private final ByteSequence rootPass = bytesOf("123");
    private final String rootRoleString = "root";
    private final ByteSequence rootRole = bytesOf(rootRoleString);

    private final String userString = "user";
    private final ByteSequence user = bytesOf(userString);
    private final ByteSequence userPass = bytesOf("userPass");
    private final String userRoleString = "userRole";
    private final ByteSequence userRole = bytesOf(userRoleString);

    @BeforeEach
    public void setUp() throws Exception {
        Auth authClient = Client.builder().endpoints(cluster.getClientEndpoints()).build().getAuthClient();

        authClient.roleAdd(rootRole).get();
        authClient.roleAdd(userRole).get();

        authClient.roleGrantPermission(
            rootRole, rootRoleKeyRangeBegin, rootRoleKeyRangeEnd, Permission.Type.READWRITE).get();
        authClient.roleGrantPermission(
            userRole, userRoleKeyRangeBegin, userRoleKeyRangeEnd, Permission.Type.READWRITE).get();

        authClient.userAdd(root, rootPass).get();
        authClient.userAdd(user, userPass).get();

        authClient.userGrantRole(root, rootRole).get();
        authClient.userGrantRole(user, rootRole).get();
        authClient.userGrantRole(user, userRole).get();

        authClient.authEnable().get();
    }

    @AfterEach
    public void tearDown() throws Exception {
        Client rootClient = Client.builder()
            .endpoints(cluster.getClientEndpoints())
            .user(root)
            .password(rootPass)
            .build();

        rootClient.getAuthClient().authDisable().get();

        Auth client = Client.builder()
            .endpoints(cluster.getClientEndpoints())
            .build()
            .getAuthClient();

        client.userDelete(root).get();
        client.userDelete(user).get();
        client.roleDelete(rootRole).get();
    }

    @Test
    public void testAuthHeaders() throws Exception {
        final CountDownLatch headerInterceptLatch = new CountDownLatch(1);
        final CountDownLatch interceptLatch = new CountDownLatch(1);

        final Client client = Client.builder()
            .endpoints(cluster.getClientEndpoints())
            .user(user)
            .password(userPass)
            .authHeader("AuthFoo", "AuthBar")
            .header("MyFoo", "MyBar")
            .interceptor(Interceptors.intercept((l, h) -> {
                assertThat(h.get(Metadata.Key.of("AuthFoo", Metadata.ASCII_STRING_MARSHALLER)))
                    .isNull();
                assertThat(h.get(Metadata.Key.of("MyFoo", Metadata.ASCII_STRING_MARSHALLER)))
                    .isEqualTo("MyBar");

                interceptLatch.countDown();
            }))
            .authInterceptors(Interceptors.intercept((l, h) -> {
                assertThat(h.get(Metadata.Key.of("AuthFoo", Metadata.ASCII_STRING_MARSHALLER)))
                    .isEqualTo("AuthBar");
                assertThat(h.get(Metadata.Key.of("MyFoo", Metadata.ASCII_STRING_MARSHALLER)))
                    .isNull();

                headerInterceptLatch.countDown();
            }))
            .build();

        try {
            client.getKVClient().put(userRoleKey, userRoleValue).get();

            headerInterceptLatch.await(1, TimeUnit.MINUTES);
            interceptLatch.await(1, TimeUnit.MINUTES);
        } finally {
            client.close();
        }
    }
}
