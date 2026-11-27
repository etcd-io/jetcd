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

/**
 * CompactedException is thrown when a operation wants to retrieve key at a revision that has
 * been compacted.
 */
public class CompactedException extends EtcdException {

    /** The revision at which compaction occurred. */
    private long compactedRevision;

    /**
     * Package-private constructor for creating compacted exceptions.
     *
     * @param code         the error code
     * @param message      the error message
     * @param compactedRev the revision at which compaction occurred
     */
    CompactedException(ErrorCode code, String message, long compactedRev) {
        super(code, message, null);
        this.compactedRevision = compactedRev;
    }

    /**
     * Returns the current compacted revision of the etcd server.
     * Revisions at or below this value have been compacted and are no longer available.
     *
     * @return the compacted revision
     */
    public long getCompactedRevision() {
        return compactedRevision;
    }
}
