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

package io.etcd.jetcd.common.suppliers;

import java.util.function.Supplier;

import io.vertx.core.Future;

/**
 * A supplier that provides AsyncCloseable instances and can itself be closed
 * asynchronously to clean up the supplied resource.
 *
 * @param <T> the type of AsyncCloseable results supplied
 */
public interface AsyncCloseableSupplier<T extends AsyncCloseable> extends Supplier<T> {
    /**
     * Closes the supplied resource if it was initialized.
     *
     * @return a Future that completes when the resource is closed
     */
    Future<Void> close();
}
