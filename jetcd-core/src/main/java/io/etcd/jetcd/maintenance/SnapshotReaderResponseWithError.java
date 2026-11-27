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

package io.etcd.jetcd.maintenance;

/**
 * Wraps a snapshot response or an error.
 */
public class SnapshotReaderResponseWithError {

    /** The snapshot response, if successful. */
    public SnapshotResponse snapshotResponse;
    /** The error, if any. */
    public Exception error;

    /**
     * Creates a wrapper with a successful response.
     *
     * @param snapshotResponse the snapshot response
     */
    public SnapshotReaderResponseWithError(SnapshotResponse snapshotResponse) {
        this.snapshotResponse = snapshotResponse;
    }

    /**
     * Creates a wrapper with an error.
     *
     * @param e the exception
     */
    public SnapshotReaderResponseWithError(Exception e) {
        this.error = e;
    }
}
