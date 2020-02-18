/*
 * Copyright 2016-2020 The jetcd authors
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

package io.etcd.jetcd.resolver;

import java.net.URI;
import java.util.Collection;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import io.grpc.NameResolver;

public class SmartNameResolverFactory extends NameResolver.Factory {
    private final String authority;
    private final Collection<URI> uris;
    private final URIResolverLoader loader;

    private SmartNameResolverFactory(String authority, Collection<URI> uris, URIResolverLoader loader) {

        Preconditions.checkNotNull(loader, "URIResolverLoader should not be null");
        Preconditions.checkNotNull(authority, "Authority should not be null");

        this.authority = authority;
        this.uris = uris;
        this.loader = loader;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if ("etcd".equals(targetUri.getScheme())) {
            return new SmartNameResolver(this.authority, this.uris, this.loader);
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return "etcd";
    }

    public static NameResolver.Factory forEndpoints(String authority, Collection<URI> endpoints, URIResolverLoader loader) {

        return new SmartNameResolverFactory(authority, endpoints, loader);
    }
}
