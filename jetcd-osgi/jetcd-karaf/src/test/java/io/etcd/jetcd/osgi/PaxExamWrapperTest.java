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

package io.etcd.jetcd.osgi;

import io.etcd.jetcd.launcher.junit4.EtcdClusterResource;
import java.net.URI;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;

/**
 * Pax Exam test wrapper which starts (and stops) an etcd test server via
 * launcher. We need to do this because jetcd-launcher uses Testcontainers,
 * which cannot run inside OSGi (due to the use of TCL loadClass(String) in
 * DockerClientProviderStrategy). And even if we were to use the launcher only
 * in the config() method (which "is executed before the OSGi container is
 * launched, so it does run in plain Java"), the Pax Exam probe still needs to
 * load the entire test class and all of its references (launcher with
 * Testcontainers) into OSGi, which is a PITA. There is also no easy way to stop
 * the testcontainer after. It is therefore simplest to just launch the etcd
 * server before getting Pax Exam's world, like this does.
 *
 * @author Michael Vorburger.ch
 */
public class PaxExamWrapperTest {

  private static final String ETCD_ENDPOINT_SYSTEM_PROPERTY_NAME = "etcd.endpoint";

  @Rule
  public final EtcdClusterResource cluster = new EtcdClusterResource("karaf");

  @Test
  public void testClientServiceChecks() throws Throwable {
    URI endpoint = cluster.getClientEndpoints().get(0);
    System.setProperty(ETCD_ENDPOINT_SYSTEM_PROPERTY_NAME, endpoint.toString());

    Optional<Failure> failure = JUnitCore.runClasses(ClientServiceChecks.class).getFailures().stream().findFirst();
    if (failure.isPresent()) {
      throw failure.get().getException();
    }
  }

  static String getClientEndpoints() {
    return System.getProperty(ETCD_ENDPOINT_SYSTEM_PROPERTY_NAME);
  }
}
