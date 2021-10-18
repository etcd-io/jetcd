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

import io.etcd.jetcd.auth.AuthDisableResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.grpc.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.etcd.jetcd.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientConnectionManagerTest {

    private final String rootString = "root";
    private final ByteSequence root = bytesOf(rootString);
    private final ByteSequence rootPass = bytesOf("123");

    @RegisterExtension
    public static final EtcdClusterExtension cluster = new EtcdClusterExtension("connection-manager-etcd", 1);

    @Test
    public void test() throws InterruptedException, ExecutionException {
        final CountDownLatch latch = new CountDownLatch(1);

        final ClientBuilder builder = Client.builder().endpoints(cluster.getClientEndpoints())
            .header("MyHeader1", "MyHeaderVal1").header("MyHeader2", "MyHeaderVal2").interceptor(new ClientInterceptor() {
                @Override
                public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions, Channel next) {
                    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                        next.newCall(method, callOptions)) {
                        @Override
                        public void start(Listener<RespT> responseListener, Metadata headers) {
                            super.start(responseListener, headers);
                            assertThat(headers.get(Metadata.Key.of("MyHeader1", Metadata.ASCII_STRING_MARSHALLER)))
                                .isEqualTo("MyHeaderVal1");
                            assertThat(headers.get(Metadata.Key.of("MyHeader2", Metadata.ASCII_STRING_MARSHALLER)))
                                .isEqualTo("MyHeaderVal2");

                            latch.countDown();
                        }
                    };
                }
            });

        try (Client client = builder.build()) {
            CompletableFuture<PutResponse> future = client.getKVClient().put(bytesOf("sample_key"), bytesOf("sample_key"));
            latch.await(1, TimeUnit.MINUTES);
            future.get();
        }
    }

    @Test
    public void testAuthHeader() throws InterruptedException, ExecutionException {
        final CountDownLatch latch = new CountDownLatch(1);
        Auth authClient = Client.builder().endpoints(cluster.getClientEndpoints()).build().getAuthClient();
        authClient.userAdd(root, rootPass).get();
        ByteSequence role = TestUtil.bytesOf("root");
        authClient.userGrantRole(root, role).get();
        authClient.authEnable().get();
        final ClientBuilder builder = Client.builder().endpoints(cluster.getClientEndpoints())
            .authHeader("MyAuthHeader", "MyAuthHeaderVal").header("MyHeader2", "MyHeaderVal2")
            .user(root).password(rootPass);
        assertThat(builder.authHeaders().get(Metadata.Key.of("MyAuthHeader", Metadata.ASCII_STRING_MARSHALLER)))
            .isEqualTo("MyAuthHeaderVal");
        try (Client client = builder.build()) {
            CompletableFuture<AuthDisableResponse> future = client.getAuthClient().authDisable();
            latch.await(10, TimeUnit.SECONDS);
            future.get();
        }
        authClient.userRevokeRole(root, role).get();
        authClient.userDelete(root).get();
    }
}
