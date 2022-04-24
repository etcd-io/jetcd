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

package io.etcd.jetcd.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Constants;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import static io.etcd.jetcd.common.exception.EtcdExceptionFactory.toEtcdException;
import static io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor;

public final class Util {

    private Util() {
    }

    public static List<URI> toURIs(Collection<String> uris) {
        return uris.stream().map(uri -> {
            try {
                return new URI(uri);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid endpoint URI: " + uri, e);
            }
        }).collect(Collectors.toList());
    }

    /**
     * convert ListenableFuture of Type S to CompletableFuture of Type T.
     */
    public static <S, T> CompletableFuture<T> toCompletableFuture(ListenableFuture<S> sourceFuture,
        Function<S, T> resultConvert,
        Executor executor) {

        CompletableFuture<T> targetFuture = new CompletableFuture<T>() {
            // the cancel of targetFuture also cancels the sourceFuture.
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                super.cancel(mayInterruptIfRunning);
                return sourceFuture.cancel(mayInterruptIfRunning);
            }
        };

        sourceFuture.addListener(() -> {
            try {
                targetFuture.complete(resultConvert.apply(sourceFuture.get()));
            } catch (Exception e) {
                targetFuture.completeExceptionally(toEtcdException(e));
            }
        }, executor);

        return targetFuture;
    }

    public static ByteString prefixNamespace(ByteSequence key, ByteSequence namespace) {
        return ByteString.copyFrom(namespace.isEmpty() ? key.getBytes() : namespace.concat(key).getBytes());
    }

    public static ByteString prefixNamespace(ByteString key, ByteSequence namespace) {
        return namespace.isEmpty() ? key : ByteString.copyFrom(namespace.concat(key).getBytes());
    }

    public static ByteString prefixNamespaceToRangeEnd(ByteSequence end, ByteSequence namespace) {
        if (namespace.isEmpty()) {
            return ByteString.copyFrom(end.getBytes());
        }

        if (end.size() == 1 && end.getBytes()[0] == 0) {
            // range end is '\0', calculate the prefixed range end by (key + 1)
            byte[] prefixedEndArray = namespace.getBytes();
            boolean ok = false;
            for (int i = prefixedEndArray.length - 1; i >= 0; i--) {
                prefixedEndArray[i] = (byte) (prefixedEndArray[i] + 1);
                if (prefixedEndArray[i] != 0) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                // 0xff..ff => 0x00
                prefixedEndArray = new byte[] { 0 };
            }
            return ByteString.copyFrom(prefixedEndArray);
        } else {
            return ByteString.copyFrom(namespace.concat(end).getBytes());
        }
    }

    public static ByteString prefixNamespaceToRangeEnd(ByteString end, ByteSequence namespace) {
        if (namespace.isEmpty()) {
            return end;
        }

        if (end.size() == 1 && end.toByteArray()[0] == 0) {
            // range end is '\0', calculate the prefixed range end by (key + 1)
            byte[] prefixedEndArray = namespace.getBytes();
            boolean ok = false;
            for (int i = prefixedEndArray.length - 1; i >= 0; i--) {
                prefixedEndArray[i] = (byte) (prefixedEndArray[i] + 1);
                if (prefixedEndArray[i] != 0) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                // 0xff..ff => 0x00
                prefixedEndArray = new byte[] { 0 };
            }
            return ByteString.copyFrom(prefixedEndArray);
        } else {
            return ByteString.copyFrom(namespace.concat(end).getBytes());
        }
    }

    public static ByteString unprefixNamespace(ByteString key, ByteSequence namespace) {
        return namespace.isEmpty() ? key : key.substring(namespace.size());
    }

    public static <T extends AbstractStub<T>> T applyRequireLeader(boolean requireLeader, T stub) {
        if (!requireLeader) {
            return stub;
        }
        final Metadata md = new Metadata();
        md.put(Constants.REQUIRE_LEADER_KEY, Constants.REQUIRE_LEADER_VALUE);
        return stub.withInterceptors(newAttachHeadersInterceptor(md));
    }

    public static ThreadFactory createThreadFactory(String prefix, boolean daemon) {
        ThreadFactory backingThreadFactory = Executors.defaultThreadFactory();
        return new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = backingThreadFactory.newThread(r);
                t.setDaemon(daemon);
                // set a proper name so it is easier to find out the where the thread was created
                t.setName(prefix + t.getName());
                return t;
            }
        };
    }
}
