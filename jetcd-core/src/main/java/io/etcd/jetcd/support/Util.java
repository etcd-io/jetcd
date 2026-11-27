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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.options.OptionsUtil;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;

import com.google.protobuf.ByteString;

/**
 * Utility methods for jetcd internal use.
 */
public final class Util {

    private Util() {
    }

    /**
     * Converts a collection of URI strings to a list of URI objects.
     *
     * @param  uris the URI strings
     * @return      the list of URIs
     */
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
     * Converts a URI to a SocketAddress, using default port 2379 if not specified.
     *
     * @param  uri the URI to convert
     * @return     a SocketAddress for the URI
     */
    public static SocketAddress toSocketAddress(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("URI host cannot be null or empty: " + uri);
        }
        int port = uri.getPort() != -1 ? uri.getPort() : 2379;
        return SocketAddress.inetSocketAddress(port, host);
    }

    /**
     * Checks if a ByteSequence is null or empty.
     *
     * @param  sequence the ByteSequence to check
     * @return          true if the sequence is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(ByteSequence sequence) {
        return sequence == null || sequence.isEmpty();
    }

    /**
     * Prefixes a key with the namespace.
     *
     * @param  key       the key
     * @param  namespace the namespace
     * @return           the prefixed key
     */
    public static ByteString prefixNamespace(ByteSequence key, ByteSequence namespace) {
        return ByteString.copyFrom(namespace.isEmpty() ? key.getBytes() : namespace.concat(key).getBytes());
    }

    /**
     * Prefixes a key with the namespace.
     *
     * @param  key       the key
     * @param  namespace the namespace
     * @return           the prefixed key
     */
    public static ByteString prefixNamespace(ByteString key, ByteSequence namespace) {
        return namespace.isEmpty() ? key : ByteString.copyFrom(namespace.concat(key).getBytes());
    }

    /**
     * Prefixes a range end key with the namespace.
     *
     * @param  end       the range end
     * @param  namespace the namespace
     * @return           the prefixed range end
     */
    public static ByteString prefixNamespaceToRangeEnd(ByteSequence end, ByteSequence namespace) {
        return prefixNamespaceToRangeEnd(ByteString.copyFrom(end.getBytes()), namespace);
    }

    /**
     * Prefixes a range end key with the namespace.
     *
     * @param  end       the range end
     * @param  namespace the namespace
     * @return           the prefixed range end
     */
    public static ByteString prefixNamespaceToRangeEnd(ByteString end, ByteSequence namespace) {
        if (namespace.isEmpty()) {
            return end;
        }

        if (end.size() == 1 && end.byteAt(0) == 0) {
            return ByteString.copyFrom(OptionsUtil.prefixEndOf(namespace).getBytes());
        }
        return prefixNamespace(end, namespace);
    }

    /**
     * Removes the namespace prefix from a key.
     *
     * @param  key       the prefixed key
     * @param  namespace the namespace
     * @return           the unprefixed key
     */
    public static ByteString unprefixNamespace(ByteString key, ByteSequence namespace) {
        return namespace.isEmpty() ? key : key.substring(namespace.size());
    }

    /**
     * Convert a Vert.x Future to a CompletableFuture.
     *
     * @param      vertxFuture the Vert.x future
     * @param      <T>         the result type
     * @return                 a CompletableFuture
     * @deprecated             Use {@link Future#toCompletionStage()} and
     *                         {@link java.util.concurrent.CompletionStage#toCompletableFuture()} instead
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public static <T> CompletableFuture<T> toCompletableFuture(Future<T> vertxFuture) {
        return vertxFuture.toCompletionStage().toCompletableFuture();
    }

    /**
     * Creates a thread factory with custom prefix and daemon setting.
     *
     * @param  prefix the thread name prefix
     * @param  daemon whether threads should be daemon threads
     * @return        the thread factory
     */
    public static ThreadFactory createThreadFactory(String prefix, boolean daemon) {
        ThreadFactory backingThreadFactory = Executors.defaultThreadFactory();

        return r -> {
            Thread t = backingThreadFactory.newThread(r);
            t.setDaemon(daemon);
            // set a proper name so it is easier to find out the where the thread was created
            t.setName(prefix + t.getName());
            return t;
        };
    }

}
