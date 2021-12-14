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

import java.util.function.Consumer;

import io.grpc.stub.StreamObserver;

public final class Observers {
    private Observers() {
    }

    public static <V> StreamObserver<V> observer(Consumer<V> onNext) {
        return new StreamObserver<V>() {
            @Override
            public void onNext(V value) {
                onNext.accept(value);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onCompleted() {
            }
        };
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<V> {
        private Consumer<V> onNext;
        private Consumer<Throwable> onError;
        private Runnable onCompleted;

        public Builder<V> onNext(Consumer<V> onNext) {
            this.onNext = onNext;
            return this;
        }

        public Builder<V> onError(Consumer<Throwable> onError) {
            this.onError = onError;
            return this;
        }

        public Builder<V> onCompleted(Runnable onCompleted) {
            this.onCompleted = onCompleted;
            return this;
        }

        public StreamObserver<V> build() {
            final Consumer<V> doOnNext = this.onNext;
            final Consumer<Throwable> doOnnError = this.onError;
            final Runnable doOnnCompleted = this.onCompleted;

            return new StreamObserver<V>() {
                @Override
                public void onNext(V value) {
                    if (onNext != null) {
                        doOnNext.accept(value);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    if (doOnnError != null) {
                        doOnnError.accept(throwable);
                    }
                }

                @Override
                public void onCompleted() {
                    if (doOnnCompleted != null) {
                        doOnnCompleted.run();
                    }
                }
            };
        }

    }
}
