/*
 * Copyright 2016-2023 The jetcd authors
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

package io.etcd.jetcd.common;

/**
 * Utility class for precondition checks.
 */
public final class Preconditions {

    /**
     * Private constructor to prevent instantiation.
     */
    private Preconditions() {
    }

    /**
     * Checks that an argument condition is true.
     *
     * @param  expression               the condition to check
     * @param  errorMessage             the error message if check fails
     * @throws IllegalArgumentException if expression is false
     */
    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks that a state condition is true.
     *
     * @param  expression            the condition to check
     * @param  errorMessage          the error message if check fails
     * @throws IllegalStateException if expression is false
     */
    public static void checkState(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Checks that an object is not null.
     *
     * @param  <T>                  the type of object
     * @param  obj                  the object to check
     * @param  errorMessage         the error message if check fails
     * @return                      the non-null object
     * @throws NullPointerException if obj is null
     */
    public static <T> T requireNonNull(T obj, String errorMessage) {
        if (obj == null) {
            throw new NullPointerException(errorMessage);
        }
        return obj;
    }
}
