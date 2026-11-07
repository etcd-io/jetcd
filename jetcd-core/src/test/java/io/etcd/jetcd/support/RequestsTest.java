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

package io.etcd.jetcd.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.api.DeleteRangeRequest;
import io.etcd.jetcd.api.RangeRequest;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;

import com.google.protobuf.ByteString;

import static org.assertj.core.api.Assertions.assertThat;

class RequestsTest {
    private static final ByteSequence NAMESPACE = ByteSequence.from(new byte[] { 'a', (byte) 0xff });
    private static final ByteSequence KEY = ByteSequence.from(new byte[] { (byte) 0xff });
    private static final ByteSequence UNBOUNDED_END = ByteSequence.from(new byte[] { 0 });
    private static final ByteString EXPECTED_KEY = ByteString.copyFrom(new byte[] { 'a', (byte) 0xff, (byte) 0xff });
    private static final ByteString EXPECTED_END = ByteString.copyFromUtf8("b");

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rangeRequestStopsBeforeNamespaceSuccessor(boolean prefix) {
        GetOption option = prefix ? GetOption.builder().isPrefix(true).build()
            : GetOption.builder().withRange(UNBOUNDED_END).build();

        RangeRequest request = Requests.mapRangeRequest(KEY, option, NAMESPACE);

        assertThat(request.getKey()).isEqualTo(EXPECTED_KEY);
        assertThat(request.getRangeEnd()).isEqualTo(EXPECTED_END);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deleteRequestStopsBeforeNamespaceSuccessor(boolean prefix) {
        DeleteOption option = prefix ? DeleteOption.builder().isPrefix(true).build()
            : DeleteOption.builder().withRange(UNBOUNDED_END).build();

        DeleteRangeRequest request = Requests.mapDeleteRequest(KEY, option, NAMESPACE);

        assertThat(request.getKey()).isEqualTo(EXPECTED_KEY);
        assertThat(request.getRangeEnd()).isEqualTo(EXPECTED_END);
    }
}
