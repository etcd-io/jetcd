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

package io.etcd.jetcd.launcher;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.testcontainers.lifecycle.Startable;

public interface EtcdCluster extends Startable {

    default void restart(long delay, TimeUnit unit) throws InterruptedException {
        stop();

        if (delay > 0) {
            unit.sleep(delay);
        }

        start();
    }

    String clusterName();

    List<URI> clientEndpoints();

    List<URI> peerEndpoints();

    List<EtcdContainer> containers();
}
