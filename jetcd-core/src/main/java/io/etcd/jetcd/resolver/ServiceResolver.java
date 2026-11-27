/*
 * Copyright 2016-2025 The jetcd authors
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

import io.vertx.core.net.Address;
import io.vertx.core.net.AddressResolver;

/**
 * Encapsulates address resolution logic and the target address for etcd connections.
 *
 * @param <S> the server address type being resolved and the target address type
 */
public interface ServiceResolver<S extends Address> {
    /**
     * Returns the address resolver that handles the resolution logic.
     *
     * @return the address resolver
     */
    AddressResolver<S> getResolver();

    /**
     * Returns the target address that will be passed to the resolver.
     * This acts as the logical service address or lookup key.
     *
     * @return the target address
     */
    S getTarget();
}
