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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.grpc.stub.StreamObserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ObserversTest {

    @Test
    void addingHandlerDoesNotAffectExistingObserver() {
        List<String> values = new ArrayList<>();
        Observers.Builder<String> builder = Observers.builder();
        StreamObserver<String> original = builder.build();
        StreamObserver<String> updated = builder.onNext(values::add).build();

        assertThatCode(() -> original.onNext("original")).doesNotThrowAnyException();
        updated.onNext("updated");

        assertThat(values).containsExactly("updated");
    }

    @Test
    void clearingHandlerDoesNotAffectExistingObserver() {
        List<String> values = new ArrayList<>();
        Observers.Builder<String> builder = Observers.<String> builder().onNext(values::add);
        StreamObserver<String> original = builder.build();
        StreamObserver<String> updated = builder.onNext(null).build();

        original.onNext("original");
        updated.onNext("updated");

        assertThat(values).containsExactly("original");
    }

    @Test
    void replacingHandlerDoesNotAffectExistingObserver() {
        List<String> originalValues = new ArrayList<>();
        List<String> updatedValues = new ArrayList<>();
        Observers.Builder<String> builder = Observers.<String> builder().onNext(originalValues::add);
        StreamObserver<String> original = builder.build();
        StreamObserver<String> updated = builder.onNext(updatedValues::add).build();

        original.onNext("original");
        updated.onNext("updated");

        assertThat(originalValues).containsExactly("original");
        assertThat(updatedValues).containsExactly("updated");
    }
}
