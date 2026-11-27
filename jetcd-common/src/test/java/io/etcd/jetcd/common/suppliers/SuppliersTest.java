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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SuppliersTest {

    @Test
    void testMemoizingOptionalNonNull() {
        AtomicInteger counter = new AtomicInteger(0);

        Supplier<Optional<String>> supplier = Suppliers.memoizingOptional(() -> {
            counter.incrementAndGet();
            return "value";
        });

        assertThat(supplier.get()).isEqualTo(Optional.of("value"));
        assertThat(supplier.get()).isEqualTo(Optional.of("value"));
        assertThat(counter.get()).isEqualTo(1); // Memoized - only computed once
    }

    @Test
    void testMemoizingOptionalNull() {
        AtomicInteger counter = new AtomicInteger(0);

        Supplier<Optional<String>> supplier = Suppliers.memoizingOptional(() -> {
            counter.incrementAndGet();
            return null;
        });

        assertThat(supplier.get()).isEqualTo(Optional.empty());
        assertThat(supplier.get()).isEqualTo(Optional.empty());
        assertThat(counter.get()).isEqualTo(1); // Still memoized even for null
    }

    @Test
    void testMemoizingOptionalConditional() {
        AtomicInteger counter = new AtomicInteger(0);
        boolean[] hasValue = { true };

        Supplier<Optional<String>> supplier = Suppliers.memoizingOptional(() -> {
            counter.incrementAndGet();
            return hasValue[0] ? "value" : null;
        });

        assertThat(supplier.get()).isEqualTo(Optional.of("value"));
        assertThat(counter.get()).isEqualTo(1);

        // Change condition - but result is memoized, so still returns original
        hasValue[0] = false;
        assertThat(supplier.get()).isEqualTo(Optional.of("value"));
        assertThat(counter.get()).isEqualTo(1); // Still only computed once
    }

    @Test
    void testMemoizingOptionalToString() {
        Supplier<Optional<String>> supplier = Suppliers.memoizingOptional(() -> "test");

        assertThat(supplier.toString()).contains("Suppliers.memoizingOptional");
    }
}
