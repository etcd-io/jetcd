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

package io.etcd.jetcd.launcher;

/**
 * Exception thrown when an etcd cluster startup times out.
 * This indicates that one or more containers failed to start within
 * the configured timeout period.
 */
public class EtcdClusterTimeoutException extends EtcdClusterStartException {

    /**
     * Creates a new timeout exception with the specified message.
     *
     * @param message the detail message
     */
    public EtcdClusterTimeoutException(String message) {
        super(message);
    }

    /**
     * Creates a new timeout exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public EtcdClusterTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
