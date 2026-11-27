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

import java.util.List;
import java.util.function.Supplier;

import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.common.suppliers.Suppliers;
import io.etcd.jetcd.impl.AbstractResponse;

/**
 * MemberRemoveResponse returned by {@link Cluster#removeMember(long)}
 * contains a header and a list of member the removal of the member.
 */
public class MemberRemoveResponse extends AbstractResponse<io.etcd.jetcd.api.MemberRemoveResponse> {

    private final Supplier<List<Member>> members;

    /**
     * Creates a new MemberRemoveResponse from the gRPC response.
     *
     * @param response the gRPC member remove response
     */
    public MemberRemoveResponse(io.etcd.jetcd.api.MemberRemoveResponse response) {
        super(response, response.getHeader());

        this.members = Suppliers.memoizing(() -> Util.toMembers(getResponse().getMembersList()));
    }

    /**
     * Returns a list of all members after removing the member.
     *
     * @return the list of members.
     */
    public List<Member> getMembers() {
        return members.get();
    }
}
