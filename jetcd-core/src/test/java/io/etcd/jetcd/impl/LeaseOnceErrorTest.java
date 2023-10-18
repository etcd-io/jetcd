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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(value = 45, unit = TimeUnit.SECONDS)
public class LeaseOnceErrorTest {

    @RegisterExtension
    public static final EtcdClusterExtension CLUSTER = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    private Client client;
    private Lease leaseClient;

    private static final long TTL = 2;

    @BeforeEach
    public void setUp() {
        client = TestUtil.client(CLUSTER).build();
        leaseClient = client.getLeaseClient();
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // https://github.com/etcd-io/jetcd/issues/1254
    @Test
    public void testKeepAliveError() throws Exception {
        final long leaseID = leaseClient.grant(TTL).get(1, TimeUnit.SECONDS).getID();

        LeaseKeepAliveResponse ka1 = leaseClient.keepAliveOnce(leaseID).get(1, TimeUnit.SECONDS);
        assertThat(ka1.getTTL()).isNotZero();

        LeaseRevokeResponse r1 = leaseClient.revoke(leaseID).get(1, TimeUnit.SECONDS);
        assertThat(r1).isNotNull();

        assertThatThrownBy(() -> leaseClient.keepAliveOnce(leaseID).get(1, TimeUnit.SECONDS))
            .hasCauseInstanceOf(EtcdException.class)
            .hasMessageContaining("etcdserver: requested lease not found");
    }
}
