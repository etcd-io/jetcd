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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.api.AuthGrpc;
import io.etcd.jetcd.api.AuthenticateResponse;
import io.etcd.jetcd.api.KVGrpc;
import io.etcd.jetcd.api.PutResponse;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.util.MutableHandlerRegistry;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class AuthUnitTest {
    private static final String AUTHENTICATE_METHOD_NAME = generateFullMethodName(AuthGrpc.SERVICE_NAME, "Authenticate");

    private final ByteSequence user = bytesOf("user");
    private final ByteSequence userPass = bytesOf("pass");
    private final ByteSequence key = bytesOf("foo");
    private final ByteSequence value = bytesOf("bar");

    @Test
    public void testHeaders() throws Exception {
        MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();
        serviceRegistry.addService(new AuthGrpc.AuthImplBase() {
            @Override
            public void authenticate(
                io.etcd.jetcd.api.AuthenticateRequest request,
                io.grpc.stub.StreamObserver<io.etcd.jetcd.api.AuthenticateResponse> responseObserver) {

                responseObserver.onNext(
                    AuthenticateResponse.newBuilder().setToken("token").build());
            }
        });
        serviceRegistry.addService(new KVGrpc.KVImplBase() {
            @Override
            public void put(
                io.etcd.jetcd.api.PutRequest request,
                io.grpc.stub.StreamObserver<io.etcd.jetcd.api.PutResponse> responseObserver) {

                responseObserver.onNext(
                    PutResponse.newBuilder().build());
            }
        });

        Server server = null;
        Client client = null;

        try {
            Metadata intercepted = new Metadata();

            server = NettyServerBuilder.forPort(0)
                .fallbackHandlerRegistry(serviceRegistry)
                .intercept(new ServerInterceptor() {
                    @Override
                    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                        ServerCall<ReqT, RespT> call,
                        Metadata headers,
                        ServerCallHandler<ReqT, RespT> next) {

                        if (AUTHENTICATE_METHOD_NAME.equals(call.getMethodDescriptor().getFullMethodName())) {
                            intercepted.merge(headers);
                        }

                        return next.startCall(
                            new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                            },
                            headers);
                    }
                })
                .directExecutor()
                .build()
                .start();

            client = Client.builder()
                .endpoints(new URI("http://127.0.0.1:" + server.getPort()))
                .user(user)
                .password(userPass)
                .authHeader("foo-a", "foo-auth")
                .header("bar-h", "bar")
                .build();

            client.getKVClient().put(key, value).get(30, TimeUnit.SECONDS);

            assertThat(intercepted.keys()).contains("foo-a");
        } finally {
            if (client != null) {
                client.close();
            }
            if (server != null) {
                server.shutdownNow();
            }
        }
    }
}
