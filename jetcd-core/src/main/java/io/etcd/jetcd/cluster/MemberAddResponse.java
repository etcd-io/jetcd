/*
 * Copyright 2016-2020 The jetcd authors
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

import java.util.List;

import io.etcd.jetcd.AbstractResponse;

/**
 * MemberAddResponse returned by {@link io.etcd.jetcd.Cluster#addMember(List)}
 * contains a header, added member, and list of members after adding the new member.
 */
public class MemberAddResponse extends AbstractResponse<io.etcd.jetcd.api.MemberAddResponse> {

    private final Member member;
    private List<Member> members;

    public MemberAddResponse(io.etcd.jetcd.api.MemberAddResponse response) {
        super(response, response.getHeader());
        member = new Member(response.getMember());
    }

    /**
     * @return the member information for the added member.
     */
    public Member getMember() {
        return member;
    }

    /**
     * @return a list of all members after adding the new member.
     */
    public synchronized List<Member> getMembers() {
        if (members == null) {
            members = Util.toMembers(getResponse().getMembersList());
        }

        return members;
    }
}
