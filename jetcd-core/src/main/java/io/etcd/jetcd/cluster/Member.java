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

package io.etcd.jetcd.cluster;

import java.net.URI;
import java.util.List;

import io.etcd.jetcd.support.Util;

public class Member {

    private final io.etcd.jetcd.api.Member member;

    public Member(io.etcd.jetcd.api.Member member) {
        this.member = member;
    }

    /**
     * Returns the member ID for this member.
     */
    public long getId() {
        return member.getID();
    }

    /**
     * Returns the human-readable name of the member, ff the member is not started, the name will be an empty string.
     */
    public String getName() {
        return member.getName();
    }

    /**
     * Returns the list of URLs the member exposes to the cluster for communication.
     */
    public List<URI> getPeerURIs() {
        return Util.toURIs(member.getPeerURLsList());
    }

    /**
     * Returns list of URLs the member exposes to clients for communication, if the member is not started, clientURLs will
     * be empty.
     */
    public List<URI> getClientURIs() {
        return Util.toURIs(member.getClientURLsList());
    }
}
