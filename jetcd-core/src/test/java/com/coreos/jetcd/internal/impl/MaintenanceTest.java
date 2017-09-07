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
package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.Maintenance.Snapshot;
import com.coreos.jetcd.maintenance.StatusResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * Maintenance test.
 */
public class MaintenanceTest {

  private Client client;
  private Maintenance maintenance;
  private final Assertion test = new Assertion();

  @BeforeClass
  public void setup() {
    this.client = Client.builder().endpoints(TestConstants.endpoints).build();
    this.maintenance = client.getMaintenanceClient();
  }


  /**
   * test status member function
   */
  @Test
  public void testStatusMember() throws ExecutionException, InterruptedException {
    StatusResponse statusResponse = maintenance.statusMember(TestConstants.endpoints[0]).get();
    test.assertTrue(statusResponse.getDbSize() > 0);
  }

  // TODO: find a better way to test snapshot.
  @Test
  public void testNnapshot() throws IOException {
    // create a snapshot file current folder.
    File snapfile = Files.createTempFile("snapshot-", null).toFile();

    // leverage try-with-resources
    try (Snapshot snapshot = maintenance.snapshot();
      FileOutputStream fop = new FileOutputStream(snapfile)) {
      snapshot.write(fop);
    } catch (Exception e) {
      snapfile.delete();
    }
  }

  /**
   * test alarm list function
   * TODO trigger alarm, valid whether listAlarms will work.
   * TODO disarm the alarm member, valid whether disarm will work with listAlarms.
   */
  @Test
  public void testAlarmList() throws ExecutionException, InterruptedException {
    maintenance.listAlarms().get();
  }

  /**
   * test defragmentMember function
   */
  @Test
  void testDefragment() throws ExecutionException, InterruptedException {
    maintenance.defragmentMember(TestConstants.endpoints[0]).get();
  }

  // TODO: add test for MoveLeader when etcd cluster can be spawned per test case.
}
