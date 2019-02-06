/**
 * Copyright 2017 The jetcd authors
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

package io.etcd.jetcd.launcher.test;

import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import org.junit.Test;

/**
 * Tests (just) starting the {@link EtcdClusterFactory}.
 *
 * <p>See the EtcdClusterUsingTest in the jetcd-core artifact for more.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdClusterStartTest {

    @Test
    public void testStartEtcd() throws Exception {
        try (EtcdCluster etcd = EtcdClusterFactory.buildCluster(getClass().getSimpleName(), 3, false, false)) {
            etcd.start();
        }
    }

    @Test
    public void testStartEtcdWithAdditionalArguments() throws Exception {
        try (EtcdCluster etcd = EtcdClusterFactory.buildCluster(getClass().getSimpleName(), 3, false, false, "--max-txn-ops", "1024")) {
            etcd.start();
        }
    }
}
