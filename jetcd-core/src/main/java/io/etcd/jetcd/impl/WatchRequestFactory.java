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

package io.etcd.jetcd.impl;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.api.WatchCreateRequest;
import io.etcd.jetcd.api.WatchProgressRequest;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.options.OptionsUtil;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.Util;

/**
 * Factory for creating WatchRequest messages.
 */
final class WatchRequestFactory {

    private WatchRequestFactory() {
    }

    /**
     * Creates a WatchRequest containing a WatchCreateRequest.
     */
    static WatchRequest createRequest(
        ByteSequence key,
        ByteSequence namespace,
        WatchOption option,
        long revision) {

        WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
            .setKey(Util.prefixNamespace(key, namespace))
            .setPrevKv(option.prevKV())
            .setProgressNotify(option.progressNotify())
            .setStartRevision(revision);

        option.getEndKey()
            .map(endKey -> Util.prefixNamespaceToRangeEnd(endKey, namespace))
            .ifPresent(builder::setRangeEnd);

        if (option.getEndKey().isEmpty() && option.prefix()) {
            ByteSequence endKey = OptionsUtil.prefixEndOf(key);
            builder.setRangeEnd(Util.prefixNamespaceToRangeEnd(endKey, namespace));
        }

        if (option.noDelete()) {
            builder.addFilters(WatchCreateRequest.FilterType.NODELETE);
        }

        if (option.noPut()) {
            builder.addFilters(WatchCreateRequest.FilterType.NOPUT);
        }

        builder.setFragment(option.fragment());

        return WatchRequest.newBuilder()
            .setCreateRequest(builder.build())
            .build();
    }

    /**
     * Creates a WatchRequest containing a WatchProgressRequest.
     */
    static WatchRequest progressRequest() {
        return WatchRequest.newBuilder()
            .setProgressRequest(WatchProgressRequest.newBuilder().build())
            .build();
    }
}
