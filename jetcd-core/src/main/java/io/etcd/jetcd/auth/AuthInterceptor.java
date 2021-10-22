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

package io.etcd.jetcd.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.api.AuthGrpc;
import io.etcd.jetcd.api.AuthenticateRequest;
import io.etcd.jetcd.api.AuthenticateResponse;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.MetadataUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static io.etcd.jetcd.Util.isInvalidTokenError;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.handleInterrupt;
import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;

/**
 * AuthTokenInterceptor fills header with Auth token of any rpc calls and
 * refreshes token if the rpc results an invalid Auth token error.
 */
public class AuthInterceptor implements ClientInterceptor {
    private static final Metadata.Key<String> TOKEN = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);

    private final Object lock;
    private final ClientBuilder builder;
    private final ClientInterceptor[] interceptors;
    private volatile String token;

    public AuthInterceptor(ClientBuilder builder) {
        this.lock = new Object();
        this.builder = builder;

        List<ClientInterceptor> interceptorsChain = new ArrayList<>();
        if (builder.authHeaders() != null) {
            Metadata metadata = new Metadata();
            builder.authHeaders().forEach((BiConsumer<Metadata.Key, Object>) metadata::put);

            interceptorsChain.add(MetadataUtils.newAttachHeadersInterceptor(metadata));
        }
        if (builder.authInterceptors() != null) {
            interceptorsChain.addAll(builder.authInterceptors());
        }

        this.interceptors = interceptorsChain.isEmpty() ? null : interceptorsChain.toArray(new ClientInterceptor[0]);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = getToken(next);
                if (token != null) {
                    headers.put(TOKEN, token);
                }
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (isInvalidTokenError(status)) {
                            try {
                                refreshToken(next);
                            } catch (Exception e) {
                                // don't throw any error here.
                                // rpc will retry on expired auth token.
                            }
                        }
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }

    public void refresh() {
        synchronized (lock) {
            token = null;
        }
    }

    /**
     * get token from etcd with name and password.
     *
     * @param  channel channel to etcd
     * @return         authResp
     */
    @SuppressWarnings("rawtypes")
    private ListenableFuture<AuthenticateResponse> authenticate(@Nonnull Channel channel) {

        final ByteString user = ByteString.copyFrom(builder.user().getBytes());
        final ByteString pass = ByteString.copyFrom(builder.password().getBytes());

        checkArgument(!user.isEmpty(), "username can not be empty.");
        checkArgument(!pass.isEmpty(), "password can not be empty.");

        AuthGrpc.AuthFutureStub authFutureStub = AuthGrpc.newFutureStub(channel);
        if (interceptors != null) {
            authFutureStub = authFutureStub.withInterceptors(interceptors);
        }

        return authFutureStub.authenticate(
            AuthenticateRequest.newBuilder()
                .setNameBytes(user)
                .setPasswordBytes(pass)
                .build());
    }

    @Nullable
    private String getToken(Channel channel) {
        if (token == null) {
            synchronized (lock) {
                if (token == null) {
                    token = generateToken(channel);
                }
            }
        }

        return token;
    }

    private void refreshToken(Channel channel) {
        synchronized (lock) {
            token = generateToken(channel);
        }
    }

    /**
     * get token with ClientBuilder.
     *
     * @return                                              the auth token
     * @throws io.etcd.jetcd.common.exception.EtcdException a exception indicates failure reason.
     */
    @Nullable
    private String generateToken(Channel channel) {
        if (builder.user() != null && builder.password() != null) {
            try {
                return authenticate(channel).get().getToken();
            } catch (InterruptedException ite) {
                throw handleInterrupt(ite);
            } catch (ExecutionException exee) {
                throw toEtcdException(exee);
            }
        }
        return null;
    }
}
