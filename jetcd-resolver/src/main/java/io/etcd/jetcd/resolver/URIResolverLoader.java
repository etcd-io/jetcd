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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

@FunctionalInterface
public interface URIResolverLoader {

    /**
     * Loads the available URIResolver.
     *
     * @return a list of {@link URIResolver} or empty if no resolvers are found.
     */
    Collection<URIResolver> load();

    /**
     * Creates a default implementation based on {@link ServiceLoader}.
     *
     * @return the default implementation.
     */
    static URIResolverLoader defaultLoader() {
        return () -> {
            List<URIResolver> resolvers = new ArrayList<>();
            ServiceLoader.load(URIResolver.class).forEach(resolvers::add);
            return resolvers;
        };
    }
}
