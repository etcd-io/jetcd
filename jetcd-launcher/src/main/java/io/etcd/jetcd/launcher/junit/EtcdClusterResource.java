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

package io.etcd.jetcd.launcher.junit;

import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtcdClusterResource extends ExternalResource {

  private static final Logger LOG = LoggerFactory.getLogger(EtcdClusterResource.class);

  private final EtcdCluster cluster;

  public EtcdClusterResource(String clusterName) {
    this(clusterName, 1, false);
  }

  public EtcdClusterResource(String clusterName, int nodes) {
    this(clusterName, nodes, false);
  }

  public EtcdClusterResource(String clusterName, int nodes, boolean ssl) {
    this(clusterName, nodes, ssl, false);
  }

  public EtcdClusterResource(String clusterName, int nodes, boolean ssl, boolean restartable) {
    this.cluster = EtcdClusterFactory.buildCluster(clusterName, nodes, ssl, restartable);
  }

  public EtcdCluster cluster() {
    return cluster;
  }

  @Override
  protected void before() throws Throwable {
    this.cluster.start();
  }

  @Override
  protected void after() {
    try {
      this.cluster.close();
    } catch (RuntimeException e) {
      LOG.warn("close() failed (but ignoring it)", e);
    }
  }
}
