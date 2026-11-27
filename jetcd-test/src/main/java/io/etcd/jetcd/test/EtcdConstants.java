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

package io.etcd.jetcd.test;

/**
 * Common constants used across etcd tests.
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public final class EtcdConstants {

    private EtcdConstants() {
        // Utility class
    }

    /**
     * Localhost IP address used for test DNS servers and etcd connections.
     */
    public static final String LOCALHOST = "127.0.0.1";

    /**
     * Google Public DNS server IP address, useful for testing DNS resolution.
     */
    public static final String GOOGLE_DNS = "8.8.8.8";
}
