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

package io.etcd.jetcd.election;

import io.etcd.jetcd.ByteSequence;

/**
 * Represents a leader key in an election.
 *
 * @param name     the election identifier that corresponds to the leadership key
 * @param key      the opaque key representing the ownership of the election; if the key is deleted, then leadership is
 *                 lost
 * @param revision the creation revision of the key; can be used to test for ownership during transactions
 * @param lease    the lease ID of the election leader
 */
public record LeaderKey(ByteSequence name, ByteSequence key, long revision, long lease) {
}
