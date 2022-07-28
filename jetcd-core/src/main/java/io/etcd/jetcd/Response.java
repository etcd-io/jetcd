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

package io.etcd.jetcd;

/**
 * represents a generic Jetcd response.
 */
public interface Response {

    /**
     * Returns the response header
     *
     * @return the header.
     */
    Header getHeader();

    interface Header {

        /**
         * Returns the cluster id
         *
         * @return the cluster id.
         */
        long getClusterId();

        /**
         * Returns the member id
         *
         * @return the member id.
         */
        long getMemberId();

        /**
         * Returns the revision id
         *
         * @return the revision.
         */
        long getRevision();

        /**
         * Returns the raft term
         *
         * @return theraft term.
         */
        long getRaftTerm();
    }
}
