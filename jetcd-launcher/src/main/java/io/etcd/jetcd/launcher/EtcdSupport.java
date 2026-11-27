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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support utilities for etcd launcher operations.
 * Provides helper methods for file system operations and system user detection.
 */
final class EtcdSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdSupport.class);
    private static final Set<Path> DIRECTORIES_TO_CLEANUP = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);

    static {
        registerShutdownHook();
    }

    private EtcdSupport() {
    }

    private static void registerShutdownHook() {
        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.debug("JVM shutdown hook: cleaning up {} etcd data directories", DIRECTORIES_TO_CLEANUP.size());
                DIRECTORIES_TO_CLEANUP.forEach(EtcdSupport::deleteDataDirectory);
            }, "etcd-cleanup"));
        }
    }

    static void registerDirectoryForCleanup(Path dir) {
        if (dir != null) {
            DIRECTORIES_TO_CLEANUP.add(dir);
        }
    }

    static void unregisterDirectoryForCleanup(Path dir) {
        if (dir != null) {
            DIRECTORIES_TO_CLEANUP.remove(dir);
        }
    }

    /**
     * Deletes a directory and all its contents recursively.
     * Retries deletion with backoff to handle Docker container unmount timing issues.
     *
     * @param dir the directory to delete
     */
    static void deleteDataDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }

        int maxRetries = 10;
        long initialDelayMs = 10;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                deleteDirectoryContents(dir);
                if (attempt > 0) {
                    LOGGER.debug("Successfully deleted {} after {} retries", dir, attempt);
                }
                return;
            } catch (IOException e) {
                if (attempt < maxRetries - 1) {
                    long delay = initialDelayMs * (1L << attempt);
                    LOGGER.debug("Failed to delete {} (attempt {}/{}): {}. Retrying in {}ms",
                        dir, attempt + 1, maxRetries, e.getMessage(), delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOGGER.warn("Interrupted while retrying deletion of {}", dir);
                        break;
                    }
                } else {
                    LOGGER.warn("Failed to delete directory {} after {} attempts: {}", dir, maxRetries, e.getMessage());
                }
            }
        }
    }

    private static void deleteDirectoryContents(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
        }
    }

    /**
     * Determines the user specification to use for container execution.
     * Checks TC_USER environment variable first, then auto-detects from system.
     *
     * @return user specification in format "uid:gid" or "username", or null if detection fails
     */
    static String getHostUser() {
        // First check if TC_USER is set (used by CI)
        String tcUser = System.getenv("TC_USER");
        if (tcUser != null && !tcUser.isEmpty()) {
            if (isValidUserSpec(tcUser)) {
                return tcUser;
            }
            LOGGER.warn(
                "TC_USER has invalid format '{}', ignoring. Expected formats: uid:gid (e.g., 1000:1000) or username (e.g., myuser)",
                tcUser);
        }

        // Auto-detect on Unix-like systems
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            return null;
        }

        try {
            String uid = executeCommand("id", "-u");
            String gid = executeCommand("id", "-g");

            if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                return uid + ":" + gid;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not detect host user", e);
        }
        return null;
    }

    /**
     * Validates a user specification string format.
     * Accepts numeric uid:gid pairs, usernames, and username:group pairs.
     *
     * @param  userSpec the user specification to validate
     * @return          true if valid, false otherwise
     */
    static boolean isValidUserSpec(String userSpec) {
        if (userSpec == null || userSpec.isEmpty()) {
            return false;
        }
        // Valid formats:
        // - uid:gid (numeric, e.g., "1000:1000")
        // - uid (numeric, e.g., "1000")
        // - username (alphanumeric with dash/underscore, e.g., "myuser", "my-user", "my_user")
        // - username:group (e.g., "myuser:mygroup")
        return userSpec.matches("^\\d+(?::\\d+)?$|^[a-zA-Z][a-zA-Z0-9_-]*(?::[a-zA-Z][a-zA-Z0-9_-]*)?$");
    }

    /**
     * Executes a system command and returns its output.
     * Returns null if the command fails or exits with non-zero status.
     *
     * @param  command              the command and arguments to execute
     * @return                      the command output, or null on failure
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if interrupted while waiting
     */
    static String executeCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();

        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                LOGGER.debug("Command {} exited with code {}", String.join(" ", command), exitCode);
                return null;
            }

            return output;
        } finally {
            process.destroy();
        }
    }
}
