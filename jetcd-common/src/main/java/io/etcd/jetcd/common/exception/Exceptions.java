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

package io.etcd.jetcd.common.exception;

import org.slf4j.Logger;

/**
 * Utility class for handling exceptions in cleanup and resource management operations.
 */
public final class Exceptions {

    private Exceptions() {
        // Prevent instantiation
    }

    /**
     * Executes the given action, silently catching and ignoring any exceptions.
     * Useful for cleanup operations where exceptions can be safely ignored.
     *
     * @param action the action to execute
     */
    public static void quietly(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            // Silently ignore
        }
    }

    /**
     * Executes the given action, catching any exceptions and logging them at DEBUG level.
     * Useful for cleanup operations where exceptions should be logged but not propagated.
     *
     * @param action  the action to execute
     * @param logger  the logger to use for exception logging
     * @param message the log message pattern (supports SLF4J placeholders)
     * @param args    the log message arguments (exception will be appended automatically)
     */
    public static void quietly(Runnable action, Logger logger, String message, Object... args) {
        try {
            action.run();
        } catch (Exception e) {
            // Build args array with exception at the end
            Object[] allArgs = new Object[args.length + 1];
            System.arraycopy(args, 0, allArgs, 0, args.length);
            allArgs[args.length] = e;
            logger.debug(message, allArgs);
        }
    }
}
