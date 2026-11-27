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
 * Exception thrown when an etcd cluster fails to start.
 * This can be due to container startup failures, configuration errors,
 * or other issues that prevent the cluster from becoming operational.
 */
public class EtcdClusterStartException extends RuntimeException {

    /**
     * Creates a new exception with the specified message.
     *
     * @param message the detail message
     */
    public EtcdClusterStartException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public EtcdClusterStartException(String message, Throwable cause) {
        super(message, cause);
    }
}
