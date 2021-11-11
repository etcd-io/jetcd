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

package io.etcd.jetcd.resolver;

import java.net.URI;
import java.util.Objects;

import javax.annotation.Nullable;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

public abstract class AbstractResolverProvider extends NameResolverProvider {
    private final String scheme;
    private final int priority;

    public AbstractResolverProvider(String scheme, int priority) {
        this.scheme = scheme;
        this.priority = priority;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return priority;
    }

    @Override
    public String getDefaultScheme() {
        return scheme;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        return Objects.equals(scheme, targetUri.getScheme())
            ? createResolver(targetUri, args)
            : null;
    }

    protected abstract NameResolver createResolver(URI targetUri, NameResolver.Args args);
}
