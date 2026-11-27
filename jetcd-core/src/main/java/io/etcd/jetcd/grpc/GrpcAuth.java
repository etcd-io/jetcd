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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import io.etcd.jetcd.SecureByteSequence;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.support.Errors;
import io.etcd.jetcd.support.Util;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.Address;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.ServiceMethod;

import com.google.protobuf.ByteString;

import static io.etcd.jetcd.common.Preconditions.checkArgument;

/**
 * Manages authentication tokens for etcd requests.
 * Consolidates token management and GrpcClient wrapping with auth headers.
 */
public final class GrpcAuth implements AutoCloseable {
    /** Header name for authentication tokens. */
    public static final String TOKEN_HEADER = "token";

    private final GrpcService grpcService;
    private final Object tokenLock = new Object();
    private SecureByteSequence token;
    private CompletableFuture<AuthToken> authentication;
    private boolean closed;

    /**
     * Creates a new GrpcAuth instance.
     *
     * @param grpcService the gRPC service
     */
    GrpcAuth(GrpcService grpcService) {
        this.grpcService = grpcService;
    }

    /**
     * Get the current authentication token, authenticating if necessary.
     *
     * @return CompletableFuture with the token
     */
    public CompletableFuture<String> getToken() {
        return currentToken().thenApply(result -> result.value);
    }

    /**
     * Clear the cached token to force re-authentication on next request.
     */
    public void refreshToken() {
        synchronized (tokenLock) {
            clearToken();
        }
    }

    private void invalidateToken(SecureByteSequence usedToken) {
        synchronized (tokenLock) {
            // Each Authenticate reply owns a distinct cache entry, even if etcd reissues the same token.
            @SuppressWarnings("ReferenceEquality")
            boolean matches = token == usedToken;
            if (matches) {
                clearToken();
            }
        }
    }

    private void clearToken() {
        if (token != null) {
            token.close();
            token = null;
        }
    }

    /**
     * Check if authentication is configured.
     *
     * @return true if user credentials are configured
     */
    public boolean requiresAuth() {
        return !Util.isNullOrEmpty(grpcService.builder().user());
    }

    /**
     * Close and zero any cached authentication token.
     */
    @Override
    public void close() {
        CompletableFuture<AuthToken> pending;
        synchronized (tokenLock) {
            closed = true;
            clearToken();
            pending = authentication;
            authentication = null;
        }
        if (pending != null) {
            pending.completeExceptionally(new ClosedClientException("Client is closing"));
        }
    }

    /**
     * Wrap a GrpcClient to add authentication headers to all requests.
     *
     * @param  delegate the base GrpcClient to wrap
     * @return          a GrpcClient that adds auth headers
     */
    public GrpcClient wrapWithAuth(GrpcClient delegate) {
        return new AuthenticatingClient(delegate);
    }

    /**
     * Opens an authenticated request with an invalidation callback for callers that inspect the response.
     *
     * @param  method the gRPC method
     * @param  <Req>  the request type
     * @param  <Resp> the response type
     * @return        the request and its token invalidation callback
     */
    public <Req, Resp> Future<AuthenticatedRequest<Req, Resp>> request(ServiceMethod<Resp, Req> method) {
        return grpcService.getGrpcClient().request(grpcService.getServiceResolver().getTarget(), method)
            .compose(this::addHeaders);
    }

    /**
     * A gRPC request with a callback that invalidates the token used for that request.
     *
     * @param request          the gRPC request
     * @param tokenInvalidator the token invalidation callback, or null when no token was attached
     * @param <Req>            the request type
     * @param <Resp>           the response type
     */
    public record AuthenticatedRequest<Req, Resp>(GrpcClientRequest<Req, Resp> request, Runnable tokenInvalidator) {
        /**
         * Invalidates the cache entry used by this request if it is still current.
         *
         * @return whether the request used a managed token
         */
        public boolean invalidateToken() {
            if (tokenInvalidator == null) {
                return false;
            }
            tokenInvalidator.run();
            return true;
        }
    }

    private CompletableFuture<AuthToken> currentToken() {
        CompletableFuture<AuthToken> pending;
        synchronized (tokenLock) {
            if (closed) {
                return CompletableFuture.failedFuture(new ClosedClientException("Client is closing"));
            }
            if (token != null && !token.isClosed()) {
                return CompletableFuture.completedFuture(new AuthToken(token, token.toString(StandardCharsets.UTF_8)));
            }
            if (authentication != null) {
                return authentication;
            }
            pending = new CompletableFuture<>();
            authentication = pending;
        }

        // Transport creation and completion callbacks can reenter GrpcService.
        try {
            authenticate().onComplete(result -> finishAuthentication(pending, result.result(), result.cause()));
        } catch (RuntimeException error) {
            finishAuthentication(pending, null, error);
        }
        return pending;
    }

    private void finishAuthentication(CompletableFuture<AuthToken> pending, String value, Throwable error) {
        AuthToken result = null;
        synchronized (tokenLock) {
            if (closed) {
                return;
            }
            if (error == null) {
                token = SecureByteSequence.from(value);
                result = new AuthToken(token, value);
            }
            authentication = null;
        }
        if (error == null) {
            pending.complete(result);
        } else {
            pending.completeExceptionally(error);
        }
    }

    private Future<String> authenticate() {
        checkArgument(!grpcService.builder().user().isEmpty(), "username can not be empty.");
        checkArgument(!grpcService.builder().password().isEmpty(), "password can not be empty.");

        io.etcd.jetcd.api.AuthGrpcClient authClient = io.etcd.jetcd.api.AuthGrpcClient.create(
            grpcService.getGrpcClient(),
            grpcService.getServiceResolver().getTarget());

        final ByteString user = ByteString.copyFrom(this.grpcService.builder().user().getBytes());
        final ByteString pass = ByteString.copyFrom(this.grpcService.builder().password().getBytes());

        io.etcd.jetcd.api.AuthenticateRequest request = io.etcd.jetcd.api.AuthenticateRequest.newBuilder()
            .setNameBytes(user)
            .setPasswordBytes(pass)
            .build();

        return authClient.authenticate(request).map(io.etcd.jetcd.api.AuthenticateResponse::getToken);
    }

    private <Req, Resp> Future<AuthenticatedRequest<Req, Resp>> addHeaders(GrpcClientRequest<Req, Resp> request) {
        var headers = request.headers();
        grpcService.builder().headers().forEach(headers::set);

        if (!requiresAuth()) {
            return Future.succeededFuture(new AuthenticatedRequest<>(request, null));
        }

        return Future.fromCompletionStage(currentToken()).map(result -> {
            headers.set(TOKEN_HEADER, result.value);
            SecureByteSequence usedToken = result.identity;
            Runnable invalidate = () -> invalidateToken(usedToken);
            request.response().onSuccess(response -> response.end().onFailure(error -> {
                if (error instanceof InvalidStatusException status
                    && Errors.isRefreshableAuthError(status.actualStatus(), response.statusMessage())) {
                    invalidate.run();
                }
            }));
            return new AuthenticatedRequest<>(request, invalidate);
        });
    }

    private static final class AuthToken {
        private final SecureByteSequence identity;
        private final String value;

        AuthToken(SecureByteSequence identity, String value) {
            this.identity = identity;
            this.value = value;
        }
    }

    /**
     * Inner class that wraps a GrpcClient to add authentication headers.
     */
    private final class AuthenticatingClient implements GrpcClient {

        private final GrpcClient delegate;

        AuthenticatingClient(GrpcClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public <Req, Resp> Future<GrpcClientRequest<Req, Resp>> request(Address address, ServiceMethod<Resp, Req> method) {
            return delegate.request(address, method).compose(GrpcAuth.this::addHeaders).map(AuthenticatedRequest::request);
        }

        @Override
        public <Req, Resp> Future<GrpcClientRequest<Req, Resp>> request(ServiceMethod<Resp, Req> method) {
            return delegate.request(method).compose(GrpcAuth.this::addHeaders).map(AuthenticatedRequest::request);
        }

        @Override
        public Future<GrpcClientRequest<Buffer, Buffer>> request(Address address) {
            return delegate.request(address);
        }

        @Override
        public Future<GrpcClientRequest<Buffer, Buffer>> request() {
            return delegate.request();
        }

        @Override
        public Future<Void> close() {
            // GrpcService owns the wrapped transport.
            return Future.succeededFuture();
        }

    }
}
