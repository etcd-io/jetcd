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
import com.coreos.jetcd.internal.infrastructure.ClusterFactory;
import com.coreos.jetcd.internal.infrastructure.EtcdCluster;
import com.coreos.jetcd.maintenance.StatusResponse;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Maintenance test.
 */
public class MaintenanceTest {
  private static final EtcdCluster CLUSTER = ClusterFactory.buildThreeNodeCluster("maintenance-etcd");

  private Client client;
  private Maintenance maintenance;
  private final Assertion test = new Assertion();
  private List<String> endpoints;

  @BeforeClass
  public void setup() {
    endpoints = CLUSTER.getClientEndpoints();
    this.client = Client.builder().endpoints(endpoints).build();
    this.maintenance = client.getMaintenanceClient();
  }


  /**
   * test status member function
   */
  @Test
  public void testStatusMember() throws ExecutionException, InterruptedException {
    StatusResponse statusResponse = maintenance.statusMember(endpoints.get(0)).get();
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

  @Test
  public void testHashKV() throws ExecutionException, InterruptedException {
    maintenance.hashKV(endpoints.get(0), 0).get();
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
  public void testDefragment() throws ExecutionException, InterruptedException {
    maintenance.defragmentMember(endpoints.get(0)).get();
  }

  @Test
  public void testMoveLeader() throws ExecutionException, InterruptedException {
    String leaderEndpoint = null;
    List<Long> followers = new ArrayList<>();
    for(String ep : endpoints){
      StatusResponse statusResponse = maintenance.statusMember(ep).get();
      long memberId = statusResponse.getHeader().getMemberId();
      if (memberId == statusResponse.getLeader()) {
        leaderEndpoint = ep;
        continue;
      }
      followers.add(memberId);
    }
    if (leaderEndpoint == null) {
      test.fail("leader not found");
    }

    try(Client client = Client.builder().endpoints(leaderEndpoint).build()) {
      client.getMaintenanceClient().moveLeader(followers.get(0)).get();
    }
  }

  @AfterTest
  public void tearDown() throws IOException {
    CLUSTER.close();
  }
}
